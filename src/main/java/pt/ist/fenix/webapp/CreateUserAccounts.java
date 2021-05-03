package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import org.apache.commons.validator.routines.EmailValidator;
import org.fenixedu.TINValidator;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Photograph;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.photograph.Picture;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.Gender;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.IdentityCard;
import org.fenixedu.connect.domain.identification.OtherIdentityCard;
import org.fenixedu.connect.domain.identification.Passport;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.PortugueseAirForceIdentityCard;
import org.fenixedu.connect.domain.identification.PortugueseCitizenCard;
import org.fenixedu.connect.domain.identification.PortugueseIdentityCard;
import org.fenixedu.connect.domain.identification.PortugueseMilitaryIdentityCard;
import org.fenixedu.connect.domain.identification.PortugueseNavyIdentityCard;
import org.fenixedu.connect.domain.identification.PortugueseResidenceAuthorization;
import org.fenixedu.connect.util.ConnectError;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.joda.time.format.ISODateTimeFormat;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Task(englishTitle = "Create user accounts, automerge and other magic", readOnly = true)
public class CreateUserAccounts extends CronTask {

    private Map<String, Account> accountMap = null;
    private int createdAccounts = 0;
    private int connectedToExistingAccounts = 0;
    private int connectedAccounts = 0;
    private int validatedAccounts = 0;

    @Override
    public void runTask() throws Exception {
        accountMap = ConnectSystem.getInstance().getAccountSet().stream()
                .collect(Collectors.toMap(a -> a.getEmail(), a -> a));
        createdAccounts = 0;

        Bennu.getInstance().getUserSet().stream()
                .parallel()
                .forEach(this::createAccount);
        taskLog("Create %s accounts.%n", createdAccounts);
        taskLog("Connected %s users to existing accounts.%n", connectedToExistingAccounts);

        ConnectSystem.getInstance().getAccountSet().stream()
                .parallel()
                .forEach(this::autoValidate);
        taskLog("Connected %s accounts.%n", connectedAccounts);
        taskLog("Validated %s accounts.%n", validatedAccounts);

        ConnectSystem.getInstance().getIdentitySet().stream()
                .parallel()
                .forEach(this::autoMerge);

        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> identity.getUser() == null)
                .forEach(identity -> fix(identity));
    }

    private void fix(final Identity identity) {
        FenixFramework.atomic(() -> {
            if (usersFor(identity).count() == 1l) {
                final User user = usersFor(identity).findAny().get();
                identity.setUser(user);;
                taskLog("Fixed: %s%n", user.getUsername());
            }
        });
    }

    private Stream<User> usersFor(final Identity identity) {
        return identity.getAccountSet().stream()
                .map(account -> account.getUser())
                .filter(user -> user != null)
                .distinct();
    }

    private void autoMerge(final Identity identity) {
        try {
            FenixFramework.atomic(() -> {
                if (!isRecent(identity)) {
                    return;
                }
                identity.autoMerge();

                final LocalDate dateOfBirth = dateOfBirthFor(identity);
                if (dateOfBirth != null) {
                    final Set<String> names = nameSetFor(identity);
                    final String documentNumber = documentNumber(identity);
                    ConnectSystem.getInstance().getAccountSet().stream()
                            .filter(account -> account.getIdentity() == null && account.getUser() != null)
                            .filter(account -> account.getPersonalInformation() != null)
                            .filter(account -> dateOfBirth.equals(account.getPersonalInformation().getDateOfBirth()))
                            .forEach(account -> {
                                if (documentNumber != null && documentNumber.equals(documentNumber(account.getPersonalInformation()))) {
                                    account.setIdentity(identity);
                                } else if (names != null) {
                                    final String[] otherNames = nameFor(account.getPersonalInformation());
                                    if (otherNames != null && match(names, otherNames)) {
                                        taskLog("Possible match: %s and %s%n",
                                                identity.getUser() == null ? identity.getExternalId() : identity.getUser().getUsername(),
                                                account.getUser().getUsername());
                                    }
                                }
                            });
                    ConnectSystem.getInstance().getIdentitySet().stream()
                            .filter(otherIdentity -> otherIdentity != identity)
                            .filter(otherIdentity -> dateOfBirth.equals(dateOfBirthFor(otherIdentity)))
                            .forEach(otherIdentity -> {
                                if (documentNumber != null &&
                                        (documentNumber.equals(documentNumber(otherIdentity.getPersonalInformation()))
                                                || otherIdentity.getAccountSet().stream()
                                                .map(account -> account.getPersonalInformation())
                                                .map(information -> documentNumber(information))
                                                .anyMatch(d -> documentNumber.equals(d)))) {
                                    identity.merge(otherIdentity);
                                } else if (names != null
                                        && !identity.getPossibleIdentitySet().contains(otherIdentity)
                                        && !identity.getNotIdentitySet().contains(otherIdentity)) {
                                    final String[] otherNames = nameFor(otherIdentity);
                                    if (otherNames != null && match(names, otherNames)) {
                                        identity.setConnectSystemFromConnectAttemptQueue(ConnectSystem.getInstance());
                                        identity.addPossibleIdentity(otherIdentity);
                                    }
                                }
                            });
                }
            });
        } catch (final ClassCastException ex) {
            taskLog("Failled to automerge %s%n", identity.getExternalId());
        }
    }

    private boolean isRecent(final Identity identity) {
        final User user = identity.getUser();
        if (user != null) {
            if (user.getCreated().plusMonths(2).isAfterNow()) {
                return true;
            }
        }
        if (identity.getAccountSet().stream()
                .map(account -> account.getConfirmationCode())
                .filter(confirmationCode -> confirmationCode != null)
                .map(confirmationCode -> confirmationCode.getConfirmationValidUntil().plusMonths(2))
                .anyMatch(dateTime -> dateTime.isAfterNow())) {
            return true;
        }
        return false;
    }

    private static boolean match(final Set<String> s1s, final String[] s2s) {
        int matchCount = 0;
        for (final String s1 : s1s) {
            for (final String s2 : s2s) {
                if (s1.equalsIgnoreCase(s2)) {
                    matchCount++;
                }
            }
        }
        return s1s.size() + s2s.length - matchCount - matchCount < matchCount;
    }

    private Set<String> nameSetFor(final Identity identity) {
        final String[] names = nameFor(identity);
        return names == null ? null : Arrays.stream(names).collect(Collectors.toSet());
    }

    private String[] nameFor(final Identity identity) {
        PersonalInformation informationForName = identity.getPersonalInformation();
        if (informationForName == null || informationForName.getFullName().isEmpty()) {
            informationForName = identity.getAccountSet().stream()
                    .map(account -> account.getPersonalInformation())
                    .filter(i -> i != null && !i.getFullName().isEmpty())
                    .findAny().orElse(null);
        }
        return informationForName == null ? null : informationForName.getFullName().split(" ");
    }

    private String[] nameFor(final PersonalInformation information) {
        return information == null ? null : information.getFullName().split(" ");
    }

    private LocalDate dateOfBirthFor(final Identity identity) {
        PersonalInformation informationForDate = identity.getPersonalInformation();
        if (informationForDate == null || informationForDate.getDateOfBirth() == null) {
            informationForDate = identity.getAccountSet().stream()
                    .map(account -> account.getPersonalInformation())
                    .filter(i -> i != null && i.getDateOfBirth() != null)
                    .findAny().orElse(null);
        }
        return informationForDate == null ? null : informationForDate.getDateOfBirth();
    }

    private String documentNumber(final Identity identity) {
        String documentNumber = documentNumber(identity.getPersonalInformation());
        if (documentNumber == null) {
            documentNumber = identity.getAccountSet().stream()
                    .map(account -> account.getPersonalInformation())
                    .map(i -> documentNumber(i))
                    .filter(i -> i != null)
                    .findAny().orElse(null);
        }
        return documentNumber;
    }

    private String documentNumber(final PersonalInformation information) {
        final IdentificationDocument document = information == null ? null : information.getIdentificationDocument();
        return document == null ? null : document.getDocumentNumber();
    }

    private void autoValidate(final Account account) {
        FenixFramework.atomic(() -> {
            if (account.getIdentity() != null || account.getUser() == null) {
                return;
            }
            final User user = account.getUser();
            if (user.getIdentity() != null) {
                user.getIdentity().getAccountSet().add(account);
                connectedAccounts++;
            } else if (isStudentOrTeacherOrEmployee(user)) {
                Identity.getOrCreateIdentity(account);
                validatedAccounts++;
            }
        });
    }

    private boolean isStudentOrTeacherOrEmployee(final User user) {
        final Person person = user.getPerson();
        if (person != null) {
            final Teacher teacher = person.getTeacher();
            if (teacher != null) {
                if (!teacher.getAuthorizationSet().isEmpty()) {
                    return true;
                }
            }
            final Student student = person.getStudent();
            if (student != null && !student.getRegistrationsSet().isEmpty()) {
                return true;
            }
            if (!person.getPhdIndividualProgramProcessesSet().isEmpty()) {
                return true;
            }
        }
        return new ActiveEmployees().isMember(user) || new ActiveResearchers().isMember(user) || new ActiveGrantOwner().isMember(user);
    }

    private void createAccount(final User user) {
        try {
            FenixFramework.atomic(() -> {
                if (user.getAccount() != null) {
                    return;
                }
                final String email = emailsFor(user);
                if (accountMap.containsKey(email)) {
                    taskLog("Skipping user: %s because account with smae email already exists: %s%n",
                            user.getUsername(), email);
                    final Account account = accountMap.get(email);
                    if (account.getUser() == null) {
                        account.setUser(user);
                        connectedToExistingAccounts++;
                    }
                    return;
                }
                final Account account = Account.create(email, false);
                account.setUser(user);
                createdAccounts++;
                final Identity identity = user.getIdentity();
                if (identity != null) {
                    account.setIdentity(identity);
                }
                if (identity == null || identity.getPersonalInformation() == null) {
                    setPersonalInformationFromUser(user);
                }
            });
        } catch (Exception ex) {
            //don't abort script because of individual fail
            taskLog(ex.getMessage());
        }
    }

    private void setPersonalInformationFromUser(final User user) {
        final Account account = user.getAccount();
        final UserProfile profile = user.getProfile();
        final Person person = user.getPerson();
        final Gender gender = person == null || person.getGender() == null ? null :
                (person.getGender() == org.fenixedu.academic.domain.person.Gender.FEMALE ? Gender.FEMALE : Gender.MALE);
        final LocalDate dateOfBirth = person == null || person.getDateOfBirthYearMonthDay() == null ? null : person.getDateOfBirthYearMonthDay().toLocalDate();
        final String nationalityCountryCode = person == null || person.getCountry() == null ? null : person.getCountry().getCode().toUpperCase();
        final String tin = person == null || person.getSocialSecurityNumber() == null || !isValid(person.getSocialSecurityNumber()) ? null : person.getSocialSecurityNumber();
        final String addressData = null;
        final Photograph photograph = person == null ? null : person.getPersonalPhoto();
        final Picture picture = photograph == null ? null : photograph.getOriginal();
        final byte[] photoBytes = picture == null ? null : picture.getBytes();
        final String photoContentType = picture == null ? null : picture.getPictureFileFormat().getMimeType();
        final JsonObject identificationDocument = person == null ? null : readIdentificationDocumentInformation(person, nationalityCountryCode);
        try {
            account.setPersonalInformation(profile.getGivenNames(), profile.getFamilyNames(), profile.getDisplayName(),
                    gender, dateOfBirth, nationalityCountryCode, tin, null, identificationDocument, photoBytes, photoContentType);
        } catch (final ConnectError ex) {
            taskLog("%nex: %s - %s%n %s%n %s%n",
                    ex.getMessage(),
                    toString(ex.args),
                    identificationDocument == null ? "null" : identificationDocument.toString(),
                    user.getUsername()
            );
            throw ex;
        }
    }

    private boolean isValid(final String tin) {
        final String tinCountryCode = tin.length() > 2 && Character.isAlphabetic(tin.charAt(0)) && Character.isAlphabetic(tin.charAt(1)) ? tin.substring(0, 2) : null;
        return tinCountryCode != null && TINValidator.isValid(tinCountryCode, tin.substring(2));
    }

    private String toString(final String[] args) {
        final StringBuilder builder = new StringBuilder();
        if (args != null) {
            for (final String arg : args) {
                builder.append(arg);
            }
        }
        return builder.toString();
    }

    private JsonObject readIdentificationDocumentInformation(final Person person, final String nationalityCountryCode) {
        final IDDocumentType idDocumentType = person.getIdDocumentType();
        final String documentIdNumber = person.getDocumentIdNumber();
        final YearMonthDay expirationDate = person.getExpirationDateOfDocumentIdYearMonthDay();
        if (idDocumentType == null || documentIdNumber == null || expirationDate == null) {
            return null;
        }
        final JsonObject result = new JsonObject();
        result.addProperty("documentNumber", documentIdNumber);
        result.addProperty("expirationDate", expirationDate.toLocalDate().toString(ISODateTimeFormat.date()));
        if (false) {
            return null;
        } else if (idDocumentType == IDDocumentType.CITIZEN_CARD) {
            final String extraDigit = person.getIdentificationDocumentExtraDigitValue();
            final String versionNumber = person.getIdentificationDocumentSeriesNumber();
            final String secondExtraDigit = person.getIdentificationDocumentSeriesNumberValue();
            if (PortugueseCitizenCard.isValid(documentIdNumber, extraDigit, versionNumber, secondExtraDigit)) {
                result.addProperty("type", PortugueseCitizenCard.class.getSimpleName());
                result.addProperty("countryCode", "PT");
                result.addProperty("extraDigit", extraDigit);
                result.addProperty("versionNumber", versionNumber);
                result.addProperty("secondExtraDigit", secondExtraDigit);
            } else {
                result.addProperty("type", OtherIdentityCard.class.getSimpleName());
                result.addProperty("countryCode", "PT");
            }
        } else if (idDocumentType == IDDocumentType.IDENTITY_CARD) {
            final String extraDigit = person.getIdentificationDocumentExtraDigitValue();
            if (PortugueseIdentityCard.isValid(documentIdNumber, extraDigit)) {
                result.addProperty("type", PortugueseIdentityCard.class.getSimpleName());
                result.addProperty("countryCode", "PT");
                result.addProperty("extraDigit", extraDigit);
            } else {
                result.addProperty("type", OtherIdentityCard.class.getSimpleName());
                if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                    return null;
                } else {
                    result.addProperty("countryCode", nationalityCountryCode);
                }
            }
        } else if (idDocumentType == IDDocumentType.NAVY_IDENTITY_CARD) {
            result.addProperty("type", PortugueseNavyIdentityCard.class.getSimpleName());
            result.addProperty("countryCode", "PT");
        } else if (idDocumentType == IDDocumentType.AIR_FORCE_IDENTITY_CARD) {
            result.addProperty("type", PortugueseAirForceIdentityCard.class.getSimpleName());
            result.addProperty("countryCode", "PT");
        } else if (idDocumentType == IDDocumentType.MILITARY_IDENTITY_CARD) {
            result.addProperty("type", PortugueseMilitaryIdentityCard.class.getSimpleName());
            result.addProperty("countryCode", "PT");
        } else if (idDocumentType == IDDocumentType.PASSPORT) {
            result.addProperty("type", Passport.class.getSimpleName());
            if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                return null;
            } else {
                result.addProperty("countryCode", nationalityCountryCode);
            }
        } else if (idDocumentType == IDDocumentType.FOREIGNER_IDENTITY_CARD) {
            if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                return null;
            } else {
                if ("PT".equals(nationalityCountryCode)) {
                    result.addProperty("type", OtherIdentityCard.class.getSimpleName());
                } else {
                    result.addProperty("type", IdentityCard.class.getSimpleName());
                }
                result.addProperty("countryCode", nationalityCountryCode);
            }
        } else if (idDocumentType == IDDocumentType.NATIVE_COUNTRY_IDENTITY_CARD) {
            if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                return null;
            } else {
                if ("PT".equals(nationalityCountryCode)) {
                    result.addProperty("type", OtherIdentityCard.class.getSimpleName());
                } else {
                    result.addProperty("type", IdentityCard.class.getSimpleName());
                }
                result.addProperty("countryCode", nationalityCountryCode);
            }
        } else if (idDocumentType == IDDocumentType.OTHER) {
            result.addProperty("type", OtherIdentityCard.class.getSimpleName());
            if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                return null;
            } else {
                result.addProperty("countryCode", nationalityCountryCode);
            }
        } else if (idDocumentType == IDDocumentType.EXTERNAL) {
            result.addProperty("type", OtherIdentityCard.class.getSimpleName());
            if (Strings.isNullOrEmpty(nationalityCountryCode) || Planet.getEarth().getByAlfa2(nationalityCountryCode) == null) {
                return null;
            } else {
                result.addProperty("countryCode", nationalityCountryCode);
            }
        } else if (idDocumentType == IDDocumentType.RESIDENCE_AUTHORIZATION) {
            result.addProperty("type", PortugueseResidenceAuthorization.class.getSimpleName());
            result.addProperty("countryCode", "PT");
        } else {
            return null;
        }
        return result;
    }

    private String emailsFor(final User user) {
        final Person person = user.getPerson();
        if (person != null) {
            final String email = person.getPartyContactsSet().stream()
                    .filter(EmailAddress.class::isInstance)
                    .map(EmailAddress.class::cast)
                    .filter(emailAddress -> emailAddress.isValid())
                    .filter(emailAddress -> EmailValidator.getInstance().isValid(emailAddress.getValue()))
                    .filter(emailAddress -> emailAddress.isInstitutionalType())
                    .map(emailAddress -> emailAddress.getValue())
                    .findAny().orElse(null);
            if (email != null) {
                return email;
            }
        }
        return user.getUsername() + "@tecnico.ulisboa.pt";
    }

}
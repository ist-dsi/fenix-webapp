<script type="text/javascript">
    $(document).ready(function() {
        $('label[for$="interestedInSpecialNeedsInformation"]').removeClass('col-sm-2').addClass('col-sm-8').attr('style', 'text-align: left;');
        $($('label[for$="interestedInSpecialNeedsInformation"]').children()[0]).prepend('<h2>Informação acerca de Estatuto de Estudante com necessidades educativas especiais</h2>');
    });
</script>
<div class="infoop">
        <bean:message key="label.data.authorization.information" bundle="STUDENT_RESOURCES" />
</div>

<div class="infoop job-platform-info" style="margin-top: 10px;">
        <bean:message key="label.data.authorization.information.job_platform_info" bundle="STUDENT_RESOURCES" />
</div>
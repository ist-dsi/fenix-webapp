package pt.ist.fenix.webapp.api;

import pt.ist.fenixframework.backend.jvstmojb.pstm.TransactionSupport;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/_internal/fenix-framework/stats")
public class TxStatsResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @OAuthEndpoint("_internal")
    public String stats(@QueryParam("chart") @DefaultValue("false") boolean chart, @QueryParam("hours") @DefaultValue("24") int
            hoursToReport) throws SQLException {
        accessControl("#managers");
        JsonObject data = new JsonObject();

        data.addProperty("timestamp", DateTime.now().toString("dd/MM/yyyy HH:mm:ss"));
        data.addProperty("hostname", getHostName());
        data.add("current", new Gson().toJsonTree(TransactionSupport.STATISTICS));

        if (chart) {
            JsonArray array = new JsonArray();
            try (Statement stmt = TransactionSupport.getCurrentSQLConnection().createStatement()) {
                ResultSet rs =
                        stmt.executeQuery("SELECT * FROM FF$TRANSACTION_STATISTICS where STATS_WHEN > now() - interval "
                                + hoursToReport + " hour ORDER BY STATS_WHEN ASC");

                while (rs.next()) {
                    JsonObject json = new JsonObject();

                    for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                        Object obj = rs.getObject(i + 1);
                        if (obj instanceof Timestamp) {
                            obj = ((Timestamp) obj).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        }
                        if (obj instanceof Number) {
                            json.addProperty(rs.getMetaData().getColumnName(i + 1), (Number) obj);
                        } else {
                            json.addProperty(rs.getMetaData().getColumnName(i + 1), String.valueOf(obj));
                        }
                    }
                    array.add(json);
                }
                rs.close();
            }
            data.add("stored", array);
        }

        return toJson(data);
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "<Host name unknown>";
        }
    }

}

package pt.ist.fenix.webapp.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.joda.time.DateTime;

import pt.ist.fenixframework.backend.jvstmojb.pstm.TransactionSupport;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/fenix-ist/stats")
public class TxStatsResource extends BennuRestResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String stats(@Context HttpServletRequest request, @Context HttpServletResponse response) throws SQLException {
        accessControl("#managers");
        JsonObject data = new JsonObject();

        // Allow CORS requests, we are only letting managers use this anyway...
        final String referer = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", String.valueOf(referer));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setHeader("Access-Control-Allow-Headers", "Accept, Cache-Control, Pragma, Origin, Authorization, Content-Type");

        data.addProperty("timestamp", DateTime.now().toString("dd/MM/yyyy HH:mm:ss"));
        data.addProperty("hostname", getHostName());
        data.add("current", new Gson().toJsonTree(TransactionSupport.STATISTICS));

        if (Boolean.valueOf(request.getParameter("chart"))) {

            int hoursToReport = 24;

            if (!Strings.isNullOrEmpty(request.getParameter("hours"))) {
                hoursToReport = Integer.parseInt(request.getParameter("hours"));
            }
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

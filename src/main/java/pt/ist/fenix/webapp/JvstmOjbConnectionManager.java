package pt.ist.fenix.webapp;

import java.sql.Connection;

import org.fenixedu.academic.util.ConnectionManager;

import org.apache.ojb.broker.accesslayer.LookupException;

import pt.ist.fenixframework.backend.jvstmojb.pstm.TransactionSupport;

public class JvstmOjbConnectionManager extends ConnectionManager {

    @Override
    protected Connection getConnection() {
        try {
            return TransactionSupport.getOJBBroker().serviceConnectionManager().getConnection();
        } catch (LookupException e) {
            throw new Error(e);
        }
    }

}

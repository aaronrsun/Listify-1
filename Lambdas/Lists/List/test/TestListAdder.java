import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.configuration.IMockitoConfiguration;

import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestListAdder {

    @Test
    public void testListAdderValid() throws SQLException { testListAdderCoreMock(true); }

    @Test
    public void testListAdderError() throws SQLException { testListAdderCoreMock(false); }

    public void testListAdderCoreMock(boolean shouldThrow) throws SQLException {
        StatementInjector injector;
        ArrayList<Object> rsReturns = new ArrayList<>();
        rsReturns.add(1); //new listID
        try {
            injector = new StatementInjector(null, rsReturns, shouldThrow);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            assert false; //Error in test infrastructure
            return;
        }

        ListAdder listAdder = Mockito.spy(new ListAdder(injector, "cognitoID"));
        Map<String, Object> ignore = new HashMap<>();
        Map<String, Object> body = TestInputUtils.addBody(ignore);
        body.put("name", "aname");
        try {
            Object rawIDReturn  = listAdder.conductAction(body, TestInputUtils.addQueryParams(ignore), "cognitoID");
            if (!(rawIDReturn.getClass() == Integer.class)) {
                assert false;
                return;
            }
        } catch(SQLException throwables) {
            assert shouldThrow;
            throwables.printStackTrace();
        }
        if(injector.getStatementString() == null) {
            assert(false);
            return;
        }
        when(injector.getStatementString().contains("INSERT INTO List (name, owner, lastUpdated) VALUES (?, ?, ?);INSERT INTO ListSharee(listID, userID) VALUES(?, ?);[1, cognitoID]")).thenReturn(true);
        assert !(injector.getStatementString().contains("INSERT INTO List (name, owner, lastUpdated) VALUES (?, ?, ?);INSERT INTO ListSharee(listID, userID) VALUES(?, ?);[1, cognitoID]"));
    }
}

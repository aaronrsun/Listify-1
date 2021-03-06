import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.AccessControlException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class TestListDelete {


    @Test
    public void testListDeleterWOAccess() {
        testListDeleterCoreMock(false, false);
    }

    @Test
    public void testListDeleterError() {
        testListDeleterCoreMock(true, true);
    }

    public void testListDeleterCoreMock(boolean shouldThrow, boolean hasAccess) {
        StatementInjector injector;
        ArrayList<Object> rsReturns = new ArrayList<>();
        rsReturns.add("cognitoID");
        try {
            if (!hasAccess) {
                rsReturns = null;
            }
            injector = new StatementInjector(null, rsReturns, shouldThrow);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            assert false; //Error in test infrastructure
            return;
        }

        ListDeleter testMock = Mockito.spy(new ListDeleter(injector, "cognitoID"));

        Map<String, Object> body = (Map<String, Object>) TestBasicHandler.buildFullSampleMap().get("body");
        HashMap<String, String> queryParams = (HashMap<String, String>) TestBasicHandler.buildFullSampleMap().get("body");
        queryParams.put("id", "30");

        try {
            when(testMock.conductAction(body, queryParams, "cognitoID")).thenReturn(shouldThrow);
            Object rawIDReturn = testMock.conductAction(body, queryParams, "cognitoID");
            assert !shouldThrow;
            assert (rawIDReturn == null);
            assert (injector.getStatementString().equals("SELECT * FROM List WHERE (owner = ? AND listID = ?);DELETE FROM ListSharee where listID = ?;DELETE FROM ListProduct where listID = ?;DELETE FROM List WHERE listID = ?;[30]"));
        } catch (SQLException throwables) {
            assert shouldThrow ;
        } catch (AccessControlException accessControlException) {
            assert !hasAccess;
        }
    }
}

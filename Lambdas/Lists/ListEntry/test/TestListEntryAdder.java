import org.junit.Test;
import org.mockito.Mockito;

import java.security.AccessControlException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TestListEntryAdder {

    @Test
    public void testListEntryAdderValid() {
        testListEntryAdderCoreMock(true);
    }

    @Test
    public void testListEntryAdderError() {
        testListEntryAdderCoreMock(false);
    }

    public void testListEntryAdderCoreMock(boolean shouldThrow) {
        StatementInjector injector;
        try {
            injector = new StatementInjector(null, null, shouldThrow);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            assert false; //Error in test infrastructure
            return;
        }
        ListEntryAdder listEntryAdder = Mockito.spy(new ListEntryAdder(injector, "cognitoID"));
        Map<String, Object> ignore = new HashMap<>();
        Map<String, Object> body = TestInputUtils.addBody(ignore);
        body.put("productID", 16);
        body.put("listID", 15);
        body.put("quantity", 14);
        body.put("purchased", false);

        try {
            Object rawIDReturn = listEntryAdder.conductAction(body, TestInputUtils.addQueryParams(ignore), "cognitoID");
            assert !shouldThrow;
            assert (rawIDReturn == null);
            assert (injector.getStatementString().contains("INSERT INTO ListProduct (productID, listID, quantity, addedDate, purchased) VALUES (?, ?, ?, ?, ?)[16, 15, 14, "));
            assert (injector.getStatementString().contains(", false]"));
        } catch (SQLException throwables) {
            assert shouldThrow;
            throwables.printStackTrace();
        } catch (AccessControlException throwables) {
            assert !shouldThrow;
            throwables.printStackTrace();
        }
    }
}

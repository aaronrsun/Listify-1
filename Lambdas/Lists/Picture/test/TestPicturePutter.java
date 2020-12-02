import org.junit.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TestPicturePutter {

    @Test
    public void testPicturePutterValid() {
        testPicturePutter(false);
    }

    @Test
    public void testPicturePutterError() {
        testPicturePutter(true);
    }

    public void testPicturePutter(boolean shouldThrow) {
        StatementInjector injector;
        try {
            injector = new StatementInjector(null, null, shouldThrow);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            assert false; //Error in test infrastructure
            return;
        }
        PicturePutter picturePutter = Mockito.spy(new PicturePutter(injector, "cognitoID"));
        Map<String, Object> ignore = new HashMap<>();
        Map<String, Object> body = TestInputUtils.addBody(ignore);
        try {
            Object rawIDReturn = picturePutter.conductAction(body, TestInputUtils.addQueryParams(ignore), "cognitoID");
            assert (rawIDReturn == null);
        } catch (SQLException throwables) {
            assert shouldThrow;
            throwables.printStackTrace();
        }
    }
}

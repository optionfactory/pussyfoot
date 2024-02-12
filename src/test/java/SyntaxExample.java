
import net.optionfactory.pussyfoot.hibernate.executors.EqualExecutor;
import net.optionfactory.pussyfoot.hibernate.executors.ComparatorExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.Psf;
import net.optionfactory.pussyfoot.extjs.ExtJs;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;
import org.hibernate.SessionFactory;

public class SyntaxExample {

    public static class User {
    }

    public void experiments() {
        SessionFactory hibernate = null;
        ObjectMapper mapper = null;
        Psf<User> psf = new Builder<User>()
                .onFilterRequest("exactId", Integer.class)
                /**/.applyExecutor(new EqualExecutor<>())
                /**/.onColumn("id")
                .onFilterRequest("id", String.class)
                /**/.mappedTo(ExtJs.comparator(Integer.class, mapper))
                /**/.applyExecutor(new ComparatorExecutor<>())
                /**/.onColumn("id")
                //                    .onFilterRequest("search", String.class).applyExecutor()
                .build(User.class, (query, cb, r) -> r.get("id"), hibernate);

    }

}

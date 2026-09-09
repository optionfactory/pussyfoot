import net.optionfactory.pussyfoot.Psf;
import net.optionfactory.pussyfoot.extjs.ExtJs;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;
import net.optionfactory.pussyfoot.hibernate.executors.ComparatorExecutor;
import net.optionfactory.pussyfoot.hibernate.executors.EqualExecutor;
import org.hibernate.SessionFactory;
import tools.jackson.databind.json.JsonMapper;

public class SyntaxExample {

    public static class User {
    }

    public void experiments() {
        SessionFactory hibernate = null;
        JsonMapper mapper = null;
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

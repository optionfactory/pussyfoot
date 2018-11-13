
import net.optionfactory.pussyfoot.hibernate.predicates.Equal;
import net.optionfactory.pussyfoot.hibernate.predicates.Comparator;
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
                /**/.applyPredicate(new Equal<>())
                /**/.onColumn("id")
                .onFilterRequest("id", String.class)
                /**/.mappedTo(ExtJs.comparator(Integer.class, mapper))
                /**/.applyPredicate(new Comparator<>())
                /**/.onColumn("id")
                //                    .onFilterRequest("search", String.class).applyPredicate()
                .build(User.class, hibernate);

    }

}


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
                /**/.applyFilter(new EqualPredicateBuilder<>())
                /**/.onColumn("id")
                .onFilterRequest("id", String.class)
                /**/.mappedTo(ExtJs.<Integer>comparator(mapper))
                /**/.applyFilter(new ComparatorPredicateBuilder<>())
                /**/.onColumn("id")
                //                    .onFilterRequest("search", String.class).applyFilter()
                .build(User.class, hibernate);

    }

}

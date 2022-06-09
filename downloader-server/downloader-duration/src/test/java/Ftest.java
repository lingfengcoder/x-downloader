import com.lingfeng.dutation.store.DbStore;
//import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author: wz
 * @Date: 2022/5/24 10:40
 * @Description:
 */

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({"classpath:application.xml"})
public class Ftest {


    @Autowired
    private DbStore dbStore;

    public void test() {
        //dbStore.tans();
    }

}

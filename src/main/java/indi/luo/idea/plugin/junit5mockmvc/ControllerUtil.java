package indi.luo.idea.plugin.junit5mockmvc;

import com.github.apigcc.core.schema.Book;
import com.github.apigcc.core.schema.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.apigcc.core.Apigcc;
import com.github.apigcc.core.Context;

import java.nio.file.Paths;

public class ControllerUtil {

    public static Project analyzeController(String controllerPath, String modulePath) {
        Context context = new Context();
        context.setId("111");
        context.setName("111-1");
        context.addSource(Paths.get(controllerPath).getParent());
        context.addDependency(Paths.get(modulePath));

        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
//        ClassLoader pluginClassLoader = this.getClass().getClassLoader();
        ClassLoader pluginClassLoader = ControllerUtil.class.getClassLoader();
        Project project;
        try {
            currentThread.setContextClassLoader(pluginClassLoader);
            // code working with ServiceLoader here
            Apigcc apigcc = new Apigcc(context);
            project = apigcc.parse();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
        return project;
    }
}

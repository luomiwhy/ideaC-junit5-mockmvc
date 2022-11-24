package indi.luo.idea.plugin.junit5mockmvc;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;

public class MainAction extends BaseGenerateAction {

    public MainAction() {
        super(null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        //获取当前在操作的工程上下文
        Project project = e.getData(PlatformDataKeys.PROJECT);

        //获取当前操作的类文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        //获取当前类文件的路径
        String classPath = psiFile.getVirtualFile().getPath();

        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        if (!(psiElement instanceof PsiMethod)) {
            Messages.showMessageDialog(project, "Please focus on a Mapping method", "Generate Failed", null);
            return;
        }
        String projectBasePath = project.getBasePath();
        BaseBuilder builder = null;
        builder = new HungryPatternBuilder();
        builder.build(e);
    }
}

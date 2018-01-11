package org.appng.core.templating;


import java.util.Collection;
import java.util.Set;

import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.spring4.dialect.SpringStandardDialect;
import org.thymeleaf.standard.processor.StandardReplaceTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

public class AppNGThymeleafDialect extends SpringStandardDialect{
	
	private Collection<ThymeleafReplaceInterceptor> interceptors;
	
	public AppNGThymeleafDialect(Collection<ThymeleafReplaceInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public AppNGThymeleafDialect() {
	}

	@Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        Set<IProcessor> springStandardProcessorsSet = createSpringStandardProcessorsSet(dialectPrefix);
        IProcessor origReplaceProc = null;
        for(IProcessor proc : springStandardProcessorsSet) {
        	if(proc instanceof StandardReplaceTagProcessor){
        		origReplaceProc=proc;
        	}
        }
        if(null != origReplaceProc) {
        	springStandardProcessorsSet.remove(origReplaceProc);
        }
        springStandardProcessorsSet.add(new ThymeleafReplaceTagProcessor(TemplateMode.HTML, dialectPrefix,interceptors));
        
        
		return springStandardProcessorsSet;
    }
	
	
	

}

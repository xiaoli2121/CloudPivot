package io.cloudpivot.boot.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.cloudpivot.auth.security.JwtAuthFilter;
import io.cloudpivot.auth.security.JwtBearerRealm;
import io.cloudpivot.auth.service.AuthService;
import jakarta.servlet.Filter;

@Configuration
public class ShiroConfiguration {

    @Bean
    public Realm realm(AuthService authService) {
        return new JwtBearerRealm(authService);
    }

    @Bean
    public SecurityManager securityManager(Realm realm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realm);
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator());
        securityManager.setSubjectDAO(subjectDAO);
        SecurityUtils.setSecurityManager(securityManager);
        return securityManager;
    }

    @Bean
    public SessionStorageEvaluator sessionStorageEvaluator() {
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        return sessionStorageEvaluator;
    }

    @Bean
    public Filter shiroFilter(SecurityManager securityManager) throws Exception {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);
        factoryBean.setGlobalFilters(List.of("invalidRequest"));
        factoryBean.setFilters(Map.of("jwtAuth", new JwtAuthFilter()));
        factoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap());
        return (Filter) factoryBean.getObject();
    }

    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    private Map<String, String> filterChainDefinitionMap() {
        Map<String, String> chainDefinition = new LinkedHashMap<>();
        chainDefinition.put("/api/health", "anon");
        chainDefinition.put("/api/auth/login", "anon");
        chainDefinition.put("/api/auth/refresh", "anon");
        chainDefinition.put("/api/runtime/**", "anon");
        chainDefinition.put("/api/portal/**", "anon");
        chainDefinition.put("/swagger-ui.html", "anon");
        chainDefinition.put("/swagger-ui/**", "anon");
        chainDefinition.put("/v3/api-docs", "anon");
        chainDefinition.put("/v3/api-docs/**", "anon");
        chainDefinition.put("/api/**", "noSessionCreation, jwtAuth");
        return chainDefinition;
    }
}

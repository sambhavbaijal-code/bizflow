package com.bizflow.auth;
import com.bizflow.entity.Business;
import com.bizflow.repository.BusinessRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
@Component
public class TenantContext {
 private final BusinessRepository businesses; public TenantContext(BusinessRepository b){businesses=b;}
 private jakarta.servlet.http.HttpServletRequest request(){var a=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();if(a==null)throw new IllegalStateException("No request context.");return a.getRequest();}
 public Long id(){Object id=request().getAttribute("bizflowBusinessId");if(!(id instanceof Long))throw new IllegalStateException("Choose a business workspace first.");return (Long)id;}
 public Business business(){return businesses.findById(id()).orElseThrow(()->new IllegalStateException("Business workspace not found."));}
}

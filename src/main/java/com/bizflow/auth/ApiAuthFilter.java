package com.bizflow.auth;

import com.bizflow.repository.BusinessRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {
    private final TokenSessions sessions; private final BusinessRepository businesses;
    public ApiAuthFilter(TokenSessions s,BusinessRepository b){sessions=s;businesses=b;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().startsWith("/api/")||r.getRequestURI().equals("/api/health")||r.getRequestURI().equals("/api/auth/login")||r.getRequestURI().equals("/api/auth/register");}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        String auth=req.getHeader("Authorization");String token=auth!=null&&auth.startsWith("Bearer ")?auth.substring(7):"";
        TokenSessions.Session s=sessions.get(token);
        if(s==null){res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Please log in to continue.\"}");return;}
        req.setAttribute("bizflowUserId",s.userId);
        boolean businessManagement=req.getRequestURI().equals("/api/me")||req.getRequestURI().equals("/api/businesses")||req.getRequestURI().startsWith("/api/businesses/")||req.getRequestURI().equals("/api/auth/logout");
        if(!businessManagement){
            if(s.businessId==null){res.setStatus(409);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Choose a business workspace first.\"}");return;}
            if(businesses.findByIdAndOwner_Id(s.businessId,s.userId).isEmpty()){s.businessId=null;res.setStatus(409);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Choose a business workspace first.\"}");return;}
            req.setAttribute("bizflowBusinessId",s.businessId);
        }
        req.setAttribute("bizflowToken",token);chain.doFilter(req,res);
    }
}

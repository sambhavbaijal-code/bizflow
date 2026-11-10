package com.bizflow.auth;

import com.bizflow.entity.*;
import com.bizflow.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserAccountRepository users;private final BusinessRepository businesses;private final ProductRepository products;private final PartyRepository parties;private final PurchaseRepository purchases;private final SaleRepository sales;private final TokenSessions sessions;
    public AuthController(UserAccountRepository u,BusinessRepository b,ProductRepository p,PartyRepository pa,PurchaseRepository pu,SaleRepository s,TokenSessions ts){users=u;businesses=b;products=p;parties=pa;purchases=pu;sales=s;sessions=ts;}
    public record RegisterRequest(String name,String email,String password,String businessName){}
    public record LoginRequest(String email,String password){}
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@RequestBody RegisterRequest r){
        if(blank(r.name())||blank(r.email())||r.password()==null||r.password().length()<8||blank(r.businessName()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter your name, email, business name and a password of at least 8 characters.");
        String email=r.email().trim().toLowerCase(Locale.ROOT);if(users.findByEmailIgnoreCase(email).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"An account already exists for this email.");
        UserAccount u=new UserAccount();u.setName(r.name().trim());u.setEmail(email);u.setPasswordHash(PasswordHasher.hash(r.password()));users.save(u);
        Business b=new Business();b.setName(r.businessName().trim());b.setOwner(u);businesses.save(b);adoptLegacyData(b);
        String token=sessions.create(u.getId());sessions.get(token).businessId=b.getId();return response(token,u,b);
    }
    @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody LoginRequest r){
        UserAccount u=users.findByEmailIgnoreCase(r.email()==null?"":r.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect."));
        if(r.password()==null||!PasswordHasher.matches(r.password(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect.");
        List<Business> owned=businesses.findByOwner_IdOrderByNameAsc(u.getId());String token=sessions.create(u.getId());TokenSessions.Session s=sessions.get(token);if(owned.size()==1)s.businessId=owned.get(0).getId();return response(token,u,s.businessId==null?null:owned.get(0));
    }
    @GetMapping("/me") public Map<String,Object> me(HttpServletRequest req){Long userId=(Long)req.getAttribute("bizflowUserId");String token=(String)req.getAttribute("bizflowToken");UserAccount u=users.findById(userId).orElseThrow();TokenSessions.Session s=sessions.get(token);Business active=s.businessId==null?null:businesses.findByIdAndOwner_Id(s.businessId,userId).orElse(null);return response(token,u,active);}
    @PostMapping("/auth/logout") public Map<String,String> logout(HttpServletRequest req){String token=(String)req.getAttribute("bizflowToken");if(token!=null)sessions.remove(token);return Map.of("status","ok");}
    @PostMapping("/businesses") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createBusiness(@RequestBody Map<String,String> body,HttpServletRequest req){String name=body.getOrDefault("name","").trim();if(name.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Business name is required.");Long userId=(Long)req.getAttribute("bizflowUserId");UserAccount owner=users.findById(userId).orElseThrow();Business b=new Business();b.setOwner(owner);b.setName(name);businesses.save(b);TokenSessions.Session s=sessions.get((String)req.getAttribute("bizflowToken"));s.businessId=b.getId();return response((String)req.getAttribute("bizflowToken"),owner,b);}
    @PostMapping("/businesses/{id}/select") public Map<String,Object> selectBusiness(@PathVariable("id") Long id,HttpServletRequest req){Long userId=(Long)req.getAttribute("bizflowUserId");Business b=businesses.findByIdAndOwner_Id(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Business workspace not found."));String token=(String)req.getAttribute("bizflowToken");sessions.get(token).businessId=b.getId();return response(token,users.findById(userId).orElseThrow(),b);}
    private Map<String,Object> response(String token,UserAccount u,Business active){Map<String,Object> m=new LinkedHashMap<>();m.put("token",token);m.put("user",Map.of("id",u.getId(),"name",u.getName(),"email",u.getEmail()));m.put("businesses",businesses.findByOwner_IdOrderByNameAsc(u.getId()).stream().map(b->Map.of("id",b.getId(),"name",b.getName())).toList());m.put("activeBusiness",active==null?null:Map.of("id",active.getId(),"name",active.getName()));return m;}
    private void adoptLegacyData(Business b){var oldProducts=products.findByBusinessIsNull();oldProducts.forEach(p->p.setBusiness(b));products.saveAll(oldProducts);var oldParties=parties.findByBusinessIsNull();oldParties.forEach(p->p.setBusiness(b));parties.saveAll(oldParties);var oldPurchases=purchases.findByBusinessIsNull();oldPurchases.forEach(p->p.setBusiness(b));purchases.saveAll(oldPurchases);var oldSales=sales.findByBusinessIsNull();oldSales.forEach(p->p.setBusiness(b));sales.saveAll(oldSales);}
    private boolean blank(String s){return s==null||s.trim().isEmpty();}
}

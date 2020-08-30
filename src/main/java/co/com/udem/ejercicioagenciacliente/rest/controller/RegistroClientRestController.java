package co.com.udem.ejercicioagenciacliente.rest.controller;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import co.com.udem.ejercicioagenciacliente.domain.AutenticationRequestDTO;
import co.com.udem.ejercicioagenciacliente.domain.AutenticationResponseDTO;
import co.com.udem.ejercicioagenciacliente.domain.PropiedadDTO;
import co.com.udem.ejercicioagenciacliente.entities.UserToken;
import co.com.udem.ejercicioagenciacliente.repositories.UserTokenRepository;

@RestController
public class RegistroClientRestController {
	
	@Autowired
    RestTemplate restTemplate;
   
    @Autowired
    UserTokenRepository userTokenRepository;
   
    @Autowired
    UserToken userToken;
    
    @Autowired
    private LoadBalancerClient loadBalancer;
    
    
    private static final Logger log = LoggerFactory.getLogger(RegistroClientRestController.class);
    
    @PostMapping("/autenticar")
    public String autenticar(@RequestBody AutenticationRequestDTO autenticationRequestDTO) {
    	ServiceInstance serviceInstance=loadBalancer.choose("ejercicioagencia");
    	Long numeroIdentificacion = autenticationRequestDTO.getNumeroIdentificacion();
        String baseUrl=serviceInstance.getUri().toString();
    	//String baseUrl = "http://localhost:8080";
        baseUrl=baseUrl+"/auth/signin";
        ResponseEntity<String> postResponse = restTemplate.postForEntity(baseUrl, autenticationRequestDTO, String.class);     
        System.out.println("Respuesta: "+postResponse.getBody());
        
        Gson g = new Gson();
        AutenticationResponseDTO autenticationResponseDTO = g.fromJson(postResponse.getBody(), AutenticationResponseDTO.class);
        
               
        userToken.setUsername(autenticationResponseDTO.getUsername());
        userToken.setToken(autenticationResponseDTO.getToken());
        userToken.setNumeroIdentificacion(numeroIdentificacion);
                
        if (userTokenRepository.obtenerToken(autenticationResponseDTO.getUsername())!=null) {
        	userToken=userTokenRepository.obtenerToken(autenticationResponseDTO.getUsername());
           
        	userToken.setUsername(userToken.getUsername());
        	userToken.setToken(autenticationResponseDTO.getToken());
        	userToken.setNumeroIdentificacion(numeroIdentificacion);
            userTokenRepository.save(userToken);
            return autenticationResponseDTO.getToken();
        }   
        
           userTokenRepository.save(userToken);
        	
             return autenticationResponseDTO.getToken();
       	
    }
    
    
    @GetMapping("/propiedad/consultar")
    public List<PropiedadDTO> consultarPropiedad(@RequestParam(value = "identificacion") Long identificacion) throws JsonProcessingException {
    	
        List<PropiedadDTO> propiedadDTO = null;
        ServiceInstance serviceInstance=loadBalancer.choose("ejercicioagencia");
        String baseUrl=serviceInstance.getUri().toString();
        //String baseUrl = "http://localhost:8080";
        baseUrl=baseUrl+"/propiedad/consultar/";
             
        UserToken userToken = userTokenRepository.obtenerToken(identificacion);
              
        HttpHeaders headers = new HttpHeaders();
        
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer "+ userToken.getToken());  
        
        HttpEntity<PropiedadDTO> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl+identificacion, HttpMethod.GET, entity, String.class);
      
        try {
        	propiedadDTO = new ObjectMapper().readValue(response.getBody(), new TypeReference<List<PropiedadDTO>>() {});
        } catch (Exception e) {
          
        	log.debug("Error: consultarPropiedad",e); 
        	
        }
       
        return  propiedadDTO;
       
    }
    
    
}

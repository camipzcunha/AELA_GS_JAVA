package br.com.aela.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * CONFIGURAÇÃO: Web Service SOAP
 *
 * ─── SOA ─────────────────────────────────────────────────────────
 * Esta classe configura o endpoint SOAP do AELA conforme os
 * princípios de Arquitetura Orientada a Serviços:
 *   - Contrato explícito: WSDL gerado a partir do XSD (aela.xsd)
 *   - Interoperabilidade: qualquer cliente que conheça o WSDL pode consumir
 *   - Baixo acoplamento: REST e SOAP são serviços independentes
 *
 * ─── Como funciona ───────────────────────────────────────────────
 * 1. Spring-WS registra um MessageDispatcherServlet em /ws/*
 * 2. O WSDL é gerado automaticamente a partir do XSD (aela.xsd)
 * 3. Requisições SOAP chegam em /ws/aela.wsdl e são roteadas
 *    para o @Endpoint correto pelo @PayloadRoot
 *
 * WSDL disponível em: http://localhost:8080/ws/aela.wsdl
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    /**
     * Registra o MessageDispatcherServlet no path /ws/*
     * Este servlet é o "front controller" de todas as requisições SOAP.
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);

        // Habilita a geração automática do WSDL ao acessar /ws/*.wsdl
        servlet.setTransformWsdlLocations(true);

        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * Define o WSDL gerado automaticamente a partir do XSD.
     *
     * O nome do bean ("aela") determina a URL do WSDL:
     *   http://localhost:8080/ws/aela.wsdl
     *
     * portTypeName: nome da interface de serviço no WSDL
     * locationUri:  URL base onde o serviço está disponível
     * targetNamespace: namespace XML do serviço (deve casar com o XSD)
     */
    @Bean(name = "aela")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema aelaSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();

        // Nome da porta de serviço no WSDL
        wsdl11Definition.setPortTypeName("AelaPort");

        // URL onde o serviço SOAP responde
        wsdl11Definition.setLocationUri("/ws");

        // Namespace deve ser igual ao targetNamespace do XSD
        wsdl11Definition.setTargetNamespace("http://aela.com.br/soap");

        // Schema XSD com as definições das operações
        wsdl11Definition.setSchema(aelaSchema);

        return wsdl11Definition;
    }

    /**
     * Carrega o schema XSD de src/main/resources/aela.xsd
     * Este schema define os tipos de request e response de cada operação SOAP.
     */
    @Bean
    public XsdSchema aelaSchema() {
        return new SimpleXsdSchema(new ClassPathResource("aela.xsd"));
    }
}

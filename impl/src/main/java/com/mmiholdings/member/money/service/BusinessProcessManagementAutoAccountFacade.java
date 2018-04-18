package com.mmiholdings.member.money.service;

import com.mmiholdings.multiply.library.soap.EndpointConfig;
import com.mmiholdings.service.auto.account.bpm.stub.*;
import lombok.Data;
import lombok.extern.java.Log;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceRef;
import java.io.StringWriter;

@Dependent
@Log
@Data
public class BusinessProcessManagementAutoAccountFacade {

    @WebServiceRef(type = ExceptionsProcessService.class, wsdlLocation = "file:///opt/wildfly/bin/ExceptionsProcessService.wsdl")
    private ExceptionsProcessService accountCreationExceptionService;

    @EJB
    private MoneyServicesConfig moneyServicesConfig;

    private ObjectFactory bpmAccountStubFactory = new ObjectFactory();

    public void startBPMProcess(String policyNumber, String idNumber, String cmsClientNumber,String exceptionType,String exceptionDetails) {
        log.info(String.format("Creating bpm process [pol# %s id# %s]", policyNumber, idNumber));

        ExceptionProcessBO exceptionProcessBO =
                bpmAccountStubFactory.createExceptionProcessBO();

        ExceptionBO exceptionBO = bpmAccountStubFactory.createExceptionBO();

        exceptionProcessBO.setPolicyNumber(policyNumber);
        exceptionProcessBO.setIdNumber(idNumber);
        exceptionProcessBO.setCmsNumber(cmsClientNumber);

        exceptionBO.setExceptionDetails(exceptionDetails);
        exceptionBO.setExceptionType(exceptionType);

        exceptionProcessBO.setFailureType(exceptionBO);

        Holder<ExceptionProcessBO> holder = new Holder<>(exceptionProcessBO);

        this.logWebserviceObject("Request", this.wrapFicaVerificationIn(holder.value));

        accountCreationExceptionService.getExceptionsProcessServiceSoap().logException(holder);

        this.logWebserviceObject("Response", this.wrapFicaVerificationIn(holder.value));
    }

    @PostConstruct
    public void init() {
        EndpointConfig endpointConfig = new EndpointConfig();
        endpointConfig.setEndpointUrl(
                (BindingProvider) this.accountCreationExceptionService.getExceptionsProcessServiceSoap(),
                this.moneyServicesConfig.getBpmAccountCreationUrl());
    }

    private LogException wrapFicaVerificationIn(ExceptionProcessBO input) {
        if (input != null) {
            LogException logException = new LogException();
            logException.setExceptionProcessReq(input);
            return logException;
        }

        return null;
    }

    private void logWebserviceObject(String prefix, Object instance) {
        if (instance == null) {
            return;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(instance.getClass());
            Marshaller marshaller = context.createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal(instance, sw);
            String xmlString = sw.toString();
            log.info(String.format("%s:%s", prefix, xmlString));
        } catch (JAXBException e) {
            e.printStackTrace();
            log.warning(e.getMessage());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}

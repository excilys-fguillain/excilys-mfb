package com.ebi.formation.mfb.webservices.jaxws;

import javax.jws.WebParam;
import javax.jws.WebService;

import com.ebi.formation.mfb.entities.Person;
import com.ebi.formation.mfb.services.IPersonService;

@WebService
public interface IPersonWebService extends IPersonService {

	@Override
	public Person findPersonByUsername(@WebParam(name = "username") String username);

	@Override
	public ReturnCodePerson save(@WebParam(name = "username") String username,
			@WebParam(name = "firstname") String firstname, @WebParam(name = "lastname") String lastname,
			@WebParam(name = "password") String password);
}

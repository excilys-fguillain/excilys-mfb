package com.ebi.formation.mfb.webservices.jaxrs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.ebi.formation.mfb.webservices.dto.CompteDTO;

public interface ICompteWebService {

	@GET
	@Path("getCompteById/{compteId}")
	public abstract CompteDTO getCompteById(@PathParam("compteId") Long compteId);

	@GET
	@Path("getCompteByUsername/{username}")
	public abstract List<CompteDTO> findComptesByUsername(@PathParam("username") String username);

	@GET
	@Path("checkCompte/{username}/{compteId}")
	public abstract boolean checkCompteOwnershipByUsernameAndCompteId(@PathParam("username") String username,
			@PathParam("compteId") Long compteId);

	@GET
	@Path("getCompteByNumeroCompte/{numeroCompte}")
	public abstract CompteDTO getCompteByNumeroCompte(@PathParam("numeroCompte") String numeroCompte);
}
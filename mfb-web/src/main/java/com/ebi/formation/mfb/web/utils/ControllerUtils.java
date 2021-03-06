package com.ebi.formation.mfb.web.utils;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

/**
 * @author fguillain
 * 
 */
public final class ControllerUtils {

	public static final String OBJECT_LINK_FIL_ARIANE = "linksfilAriane";
	public static final String LINK_CLIENT = "client";
	public static final String LINK_COMPTE = "compte";
	public static final String LINK_CARTE = "carte";
	public static final String LINK_VIREMENT = "virement";
	public static final String LINK_ADMIN = "admin";

	/**
	 * Constructeur empechant la classe ControllerUtils d'être instanciée
	 */
	private ControllerUtils() {
	}

	/**
	 * Méthode permettant de rediriger vers une page d'info (erreur ou confirmation) et Home en cas de refresh
	 * 
	 * @param request
	 * @param viewName
	 * @param defaultUrl
	 * @return
	 */
	public static ModelAndView redirectPageInfoOrHome(HttpServletRequest request, String viewName, String defaultUrl) {
		ModelAndView mv = new ModelAndView(viewName);
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		if (flashMap == null) {
			mv.setView(new RedirectView(request.getContextPath() + defaultUrl));
		}
		return mv;
	}
}

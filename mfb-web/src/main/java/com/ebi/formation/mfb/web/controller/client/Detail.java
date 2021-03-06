package com.ebi.formation.mfb.web.controller.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTime;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.ebi.formation.mfb.entities.Operation;
import com.ebi.formation.mfb.servicesapi.ICompteService;
import com.ebi.formation.mfb.servicesapi.IOperationService;
import com.ebi.formation.mfb.web.exception.ResourceNotFoundException;
import com.ebi.formation.mfb.web.utils.ControllerUtils;
import com.ebi.formation.mfb.web.utils.ExcelGenerator;
import com.ebi.formation.mfb.web.utils.LinkBuilder;

/**
 * Controller gérant l'accès au détail des comptes et des opérations cartes.
 * 
 * @author excilys
 * 
 */
@Controller
@RequestMapping("/client/compte/{idCompte:\\d+}/")
public class Detail {

	private enum TypeDetail {
		COMPTE, CARTE, VIREMENT
	};

	private final Logger logger = LoggerFactory.getLogger(Detail.class);
	private static final int NB_MONTH_HISTORY = 6;
	@Autowired
	ICompteService compteService;
	@Autowired
	IOperationService operationService;

	/**
	 * Retourne la première page du détail d'un compte pour le mois en cours.
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @return
	 */
	@RequestMapping(value = "detail.html", method = RequestMethod.GET)
	public ModelAndView detailCompte(Principal principal, Locale locale, @PathVariable Long idCompte) {
		return detailCompteMois(principal, locale, idCompte, DateTime.now().getYear(), DateTime.now().getMonthOfYear());
	}

	/**
	 * Retourne la première page du détail d'un compte pour un mois donné
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/detail.html", method = RequestMethod.GET)
	public ModelAndView detailCompteMois(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month) {
		return detailCompteMoisAndPage(principal, locale, idCompte, year, month, 0);
	}

	/**
	 * Retourne le détail d'un compte pour un mois donné et pour une page donnée
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @param page
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/{page:[0-9]+}/detail.html", method = RequestMethod.GET)
	public ModelAndView detailCompteMoisAndPage(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month, @PathVariable int page) {
		// Vérifie que le compte appartient au user connecté, que le mois demandé existe, que la page demandée existe.
		if (!compteService.checkCompteOwnershipByUsernameAndCompteId(principal.getName(), idCompte)
				|| !monthInHistory(month, year) || !pageExist(idCompte, year, month, page, TypeDetail.COMPTE)) {
			throw new ResourceNotFoundException();
		}
		ModelAndView mv = new ModelAndView("detailCompte");
		YearMonth currentMonth = new YearMonth(year, month);
		long nbPages = operationService.getNumberOfPagesForOperationsWithoutCartesByMonth(idCompte, month, year);
		addToModelCommonObjects(mv, locale, idCompte, currentMonth, page, nbPages, TypeDetail.COMPTE);
		// Ajout des opérations dans le modèle
		mv.addObject("operations",
				operationService.getOperationsWithoutCarteByMonthPaginated(idCompte, month, year, page));
		// Ajout du solde carte dans le modèle
		mv.addObject("soldeCarte", operationService.getTotalOperationsCarteByMonth(idCompte, month, year));
		// Calcul de la balance
		List<Operation> allOperations = operationService.getAllOperationsByMonthByCompte(idCompte, month, year);
		BigDecimal credit = BigDecimal.ZERO;
		BigDecimal debit = BigDecimal.ZERO;
		for (Operation operation : allOperations) {
			if (operation.getMontant().signum() == 1) {
				credit = credit.add(operation.getMontant());
			} else {
				debit = debit.add(operation.getMontant());
			}
		}
		BigDecimal total = credit.add(debit);
		mv.addObject("credit", credit);
		mv.addObject("debit", debit);
		mv.addObject("total", total);
		// Ajout de des urls pour aller au mois suivant et précédent dans le modèle si ils existent
		if (hasPreviousMonth(month, year)) {
			YearMonth monthBefore = currentMonth.minusMonths(1);
			mv.addObject("urlPreviousMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
					ControllerUtils.LINK_COMPTE, idCompte.longValue(), monthBefore.getYear(),
					monthBefore.getMonthOfYear(), "detail.html"));
		}
		if (hasNextMonth(month, year)) {
			YearMonth monthAfter = currentMonth.plusMonths(1);
			mv.addObject("urlNextMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
					idCompte.longValue(), monthAfter.getYear(), monthAfter.getMonthOfYear(), "detail.html"));
		}
		mv.addObject("urlDetailCarte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
				idCompte, year, month, "carte", "detail.html"));
		mv.addObject("linksVirement", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
				idCompte.longValue(), "virement", "history.html"));
		// TODO fil d'ariane
		Map<String, String> linksfilAriane = new LinkedHashMap<String, String>();
		linksfilAriane.put("linkFilAriane.home", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, "home.html"));
		linksfilAriane.put("linkFilAriane.detailCompte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
				ControllerUtils.LINK_COMPTE, idCompte.longValue(), "detail.html"));
		mv.addObject(ControllerUtils.OBJECT_LINK_FIL_ARIANE, linksfilAriane);
		return mv;
	}

	/**
	 * Retourne la première page du détail des opérations cartes pour le mois en cours.
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @return
	 */
	@RequestMapping(value = "carte/detail.html", method = RequestMethod.GET)
	public ModelAndView detailCarte(Principal principal, Locale locale, @PathVariable Long idCompte) {
		return detailCarteMois(principal, locale, idCompte, DateTime.now().getYear(), DateTime.now().getMonthOfYear());
	}

	/**
	 * Retourne la première page du détail des opérations cartes pour un mois donné.
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/carte/detail.html", method = RequestMethod.GET)
	public ModelAndView detailCarteMois(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month) {
		return detailCarteMoisAndPage(principal, locale, idCompte, year, month, 0);
	}

	/**
	 * Retourne le détail des opérations cartes pour un mois donné et une page donnée.
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @param page
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/{page:[0-9]+}/carte/detail.html", method = RequestMethod.GET)
	public ModelAndView detailCarteMoisAndPage(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month, @PathVariable int page) {
		if (!compteService.checkCompteOwnershipByUsernameAndCompteId(principal.getName(), idCompte)
				|| !monthInHistory(month, year) || !pageExist(idCompte, year, month, page, TypeDetail.CARTE)) {
			throw new ResourceNotFoundException();
		}
		ModelAndView mv = new ModelAndView("detailCompteCarte");
		long nbPages = operationService.getNumberOfPagesForOperationsCartesByMonth(idCompte, month, year);
		YearMonth currentMonth = new YearMonth(year, month);
		addToModelCommonObjects(mv, locale, idCompte, currentMonth, page, nbPages, TypeDetail.CARTE);
		// Ajout des opérations carte dans le modèle
		mv.addObject("operations", operationService.getOperationsCarteByMonthPaginated(idCompte, month, year, page));
		// Ajout de des urls pour aller au mois suivant et précédent dans le modèle si ils existent
		if (hasPreviousMonth(month, year)) {
			YearMonth monthBefore = currentMonth.minusMonths(1);
			mv.addObject("urlPreviousMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
					ControllerUtils.LINK_COMPTE, idCompte.longValue(), monthBefore.getYear(),
					monthBefore.getMonthOfYear(), ControllerUtils.LINK_CARTE, "detail.html"));
		}
		if (hasNextMonth(month, year)) {
			YearMonth monthAfter = currentMonth.plusMonths(1);
			mv.addObject("urlNextMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
					idCompte.longValue(), monthAfter.getYear(), monthAfter.getMonthOfYear(),
					ControllerUtils.LINK_CARTE, "detail.html"));
		}
		// Ajout de l'url pour revenir au détail du compte dans le modèle
		mv.addObject("urlDetailCompte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
				idCompte, year, month, "detail.html"));
		// TODO fil d'ariane
		Map<String, String> linksfilAriane = new LinkedHashMap<String, String>();
		linksfilAriane.put("linkFilAriane.home", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, "home.html"));
		linksfilAriane.put("linkFilAriane.detailCompte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
				ControllerUtils.LINK_COMPTE, idCompte.longValue(), "detail.html"));
		linksfilAriane.put("linkFilAriane.detailCarte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
				ControllerUtils.LINK_COMPTE, idCompte.longValue(), year, month, ControllerUtils.LINK_CARTE,
				"detail.html"));
		mv.addObject(ControllerUtils.OBJECT_LINK_FIL_ARIANE, linksfilAriane);
		return mv;
	}

	/**
	 * Retourne la première page du détail d'un compte pour le mois en cours.
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @return
	 */
	@RequestMapping(value = "virement/history.html", method = RequestMethod.GET)
	public ModelAndView virementHistory(Principal principal, Locale locale, @PathVariable Long idCompte) {
		return virementHistoryByMonth(principal, locale, idCompte, DateTime.now().getYear(), DateTime.now()
				.getMonthOfYear());
	}

	/**
	 * Retourne la première page du détail d'un compte pour un mois donné
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/virement/history.html", method = RequestMethod.GET)
	public ModelAndView virementHistoryByMonth(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month) {
		return virementHistoryByMonthAndPage(principal, locale, idCompte, year, month, 0);
	}

	/**
	 * Retourne le détail d'un compte pour un mois donné et pour une page donnée
	 * 
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @param page
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/{page:[0-9]+}/virement/history.html", method = RequestMethod.GET)
	public ModelAndView virementHistoryByMonthAndPage(Principal principal, Locale locale, @PathVariable Long idCompte,
			@PathVariable int year, @PathVariable int month, @PathVariable int page) {
		// Vérifie que le mois demandé existe, que la page demandée existe.
		if (!monthInHistory(month, year) || !pageExist(idCompte, year, month, page, TypeDetail.VIREMENT)) {
			throw new ResourceNotFoundException();
		}
		ModelAndView mv = new ModelAndView("historiqueVirement");
		YearMonth currentMonth = new YearMonth(year, month);
		long nbPages = operationService.getNumberOfPagesForVirementByMonth(idCompte, month, year);
		addToModelCommonObjects(mv, locale, idCompte, currentMonth, page, nbPages, TypeDetail.VIREMENT);
		// Ajout des virements dans le modèle
		mv.addObject("virements", operationService.getVirementsByMonthPaginated(idCompte, month, year, page));
		// Ajout de des urls pour aller au mois suivant et précédent dans le modèle si ils existent
		if (hasPreviousMonth(month, year)) {
			YearMonth monthBefore = currentMonth.minusMonths(1);
			mv.addObject("urlPreviousMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
					ControllerUtils.LINK_COMPTE, idCompte, monthBefore.getYear(), monthBefore.getMonthOfYear(),
					ControllerUtils.LINK_VIREMENT, "history.html"));
		}
		if (hasNextMonth(month, year)) {
			YearMonth monthAfter = currentMonth.plusMonths(1);
			mv.addObject("urlNextMonth", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
					idCompte, monthAfter.getYear(), monthAfter.getMonthOfYear(), ControllerUtils.LINK_VIREMENT,
					"history.html"));
		}
		// TODO fil d'ariane
		Map<String, String> linksfilAriane = new LinkedHashMap<String, String>();
		linksfilAriane.put("linkFilAriane.home", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, "home.html"));
		linksfilAriane.put("linkFilAriane.detailCompte", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
				ControllerUtils.LINK_COMPTE, idCompte.longValue(), "detail.html"));
		linksfilAriane.put("linkFilAriane.virementHistory", LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
				ControllerUtils.LINK_COMPTE, idCompte.longValue(), ControllerUtils.LINK_VIREMENT, "history.html"));
		mv.addObject(ControllerUtils.OBJECT_LINK_FIL_ARIANE, linksfilAriane);
		return mv;
	}

	/**
	 * Retourne une feuille excel générée.
	 * 
	 * @param request
	 * @param response
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @param year
	 * @param month
	 * @return
	 */
	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/export.html", method = RequestMethod.GET)
	public ModelAndView exportExcel(HttpServletRequest request, HttpServletResponse response, Principal principal,
			Locale locale, @PathVariable Long idCompte, @PathVariable int year, @PathVariable int month) {
		String nomCompte = compteService.getCompteById(idCompte).getLabel();
		List<Operation> listeOperations = operationService.getAllOperationsByMonthByCompte(idCompte, month, year);
		Workbook wb = ExcelGenerator.getWorkBook(idCompte, nomCompte, month, year, listeOperations);
		/****/
		response.reset();
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"compte_" + idCompte + "(" + year + "-"
				+ month + ")" + ".xls\"");
		try {
			wb.write(response.getOutputStream());
			response.getOutputStream().flush();
		} catch (IOException e) {
			logger.debug("Erreur export Excel", e);
		}
		return null;
	}

	@RequestMapping(value = "{year:20\\d{2}}/{month:[1-9]|1[012]}/{[0-9]*}/export.html", method = RequestMethod.GET)
	public ModelAndView exportExcelWithPage(HttpServletRequest request, HttpServletResponse response,
			Principal principal, Locale locale, @PathVariable Long idCompte, @PathVariable int year,
			@PathVariable int month) {
		return exportExcel(request, response, principal, locale, idCompte, year, month);
	}

	/**
	 * Retourne une feuille excel générée.
	 * 
	 * @param request
	 * @param response
	 * @param principal
	 * @param locale
	 * @param idCompte
	 * @return
	 */
	@RequestMapping(value = "export.html", method = RequestMethod.GET)
	public ModelAndView exportExcel(HttpServletRequest request, HttpServletResponse response, Principal principal,
			Locale locale, @PathVariable Long idCompte) {
		int month = DateTime.now().getMonthOfYear();
		int year = DateTime.now().getYear();
		return exportExcel(request, response, principal, locale, idCompte, year, month);
	}

	/**
	 * Vérifie si il y a un mois précédent dans l'historique
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	private boolean hasPreviousMonth(int month, int year) {
		boolean result = false;
		YearMonth currentMonth = new YearMonth(year, month);
		if (currentMonth.plusMonths(NB_MONTH_HISTORY - 1).isAfter(YearMonth.now())) {
			result = true;
		}
		return result;
	}

	/**
	 * Vérifie si il y a un mois précédent dans l'historique
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	private boolean hasNextMonth(int month, int year) {
		boolean result = false;
		YearMonth currentMonth = new YearMonth(year, month);
		if (currentMonth.isBefore(YearMonth.now())) {
			result = true;
		}
		return result;
	}

	/**
	 * Vérifie si un mois et bien dans l'historique
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	private boolean monthInHistory(int month, int year) {
		boolean result = false;
		YearMonth currentMonth = new YearMonth(year, month);
		if (currentMonth.plusMonths(NB_MONTH_HISTORY).isAfter(YearMonth.now())
				&& (currentMonth.isBefore(YearMonth.now()) || currentMonth.isEqual(YearMonth.now()))) {
			result = true;
		}
		return result;
	}

	/**
	 * Vérifie qu'une page existe bien dans l'historique
	 * 
	 * @param idCompte
	 * @param year
	 * @param month
	 * @param page
	 * @param cardDetail
	 *            vrai si détail opération carte, faux sinon
	 * @return
	 */
	private boolean pageExist(Long idCompte, int year, int month, int page, TypeDetail typeDetail) {
		switch (typeDetail) {
			case COMPTE:
				return 0L <= page
						&& page <= operationService.getNumberOfPagesForOperationsWithoutCartesByMonth(idCompte, month,
								year);
			case CARTE:
				return 0L <= page
						&& page <= operationService.getNumberOfPagesForOperationsCartesByMonth(idCompte, month, year);
			case VIREMENT:
				return 0L <= page && page <= operationService.getNumberOfPagesForVirementByMonth(idCompte, month, year);
			default:
				return false;
		}
	}

	/**
	 * Créer les URLs pour aller dans les différentes pages du détail
	 * 
	 * @param idCompte
	 * @param year
	 * @param month
	 * @param nbPages
	 * @param cardDetail
	 *            vrai si détail opération carte, faux sinon
	 * @return
	 */
	private Map<Long, String> getPagesUrls(Long idCompte, int year, int month, long nbPages, TypeDetail typeDetail) {
		Map<Long, String> map = new LinkedHashMap<Long, String>();
		for (long indexPage = 0; indexPage < nbPages; indexPage++) {
			switch (typeDetail) {
				case COMPTE:
					map.put(indexPage, LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
							idCompte, year, month, indexPage, "detail.html"));
					break;
				case CARTE:
					map.put(indexPage, LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
							idCompte, year, month, indexPage, ControllerUtils.LINK_CARTE, "detail.html"));
					break;
				case VIREMENT:
					map.put(indexPage, LinkBuilder.getLink(ControllerUtils.LINK_CLIENT, ControllerUtils.LINK_COMPTE,
							idCompte, year, month, indexPage, ControllerUtils.LINK_VIREMENT, "history.html"));
					break;
				default:
					break;
			}
		}
		return map;
	}

	/**
	 * Créer les URLs pour aller dans les différents mois de l'historique
	 * 
	 * @param locale
	 * @param idCompte
	 * @param cardDetail
	 *            vrai si détail opération carte, faux sinon
	 * @return
	 */
	private Map<String, String> getMonthUrls(Locale locale, Long idCompte, TypeDetail typeDetail) {
		Map<String, String> mapNamesUrls = new LinkedHashMap<String, String>();
		YearMonth now = YearMonth.now();
		for (int i = 0; i <= NB_MONTH_HISTORY - 1; i++) {
			YearMonth month = now.minusMonths(i);
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM yyyy");
			DateTimeFormatter localeFmt = fmt.withLocale(locale);
			switch (typeDetail) {
				case COMPTE:
					mapNamesUrls.put(localeFmt.print(month), LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
							ControllerUtils.LINK_COMPTE, idCompte, month.getYear(), month.getMonthOfYear(),
							"detail.html"));
					break;
				case CARTE:
					mapNamesUrls.put(localeFmt.print(month), LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
							ControllerUtils.LINK_COMPTE, idCompte, month.getYear(), month.getMonthOfYear(),
							ControllerUtils.LINK_CARTE, "detail.html"));
					break;
				case VIREMENT:
					mapNamesUrls.put(localeFmt.print(month), LinkBuilder.getLink(ControllerUtils.LINK_CLIENT,
							ControllerUtils.LINK_COMPTE, idCompte, month.getYear(), month.getMonthOfYear(),
							ControllerUtils.LINK_VIREMENT, "history.html"));
					break;
				default:
					break;
			}
		}
		return mapNamesUrls;
	}

	/**
	 * Ajout au {@link Model} des objets communs au détail compte et détail carte.
	 * 
	 * @param mv
	 * @param locale
	 * @param idCompte
	 * @param currentMonth
	 * @param page
	 * @param nbPages
	 * @param cardDetail
	 */
	private void addToModelCommonObjects(ModelAndView mv, Locale locale, Long idCompte, YearMonth currentMonth,
			int page, long nbPages, TypeDetail typeDetail) {
		// Ajout du compte courrant dans le modèle
		mv.addObject("compte", compteService.getCompteById(idCompte));
		// Ajout de la date courrante dans le modèle
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM yyyy");
		DateTimeFormatter localeFmt = fmt.withLocale(locale);
		mv.addObject("currentDate", localeFmt.print(currentMonth));
		// Ajout du nombre de page du détail dans le modèle
		mv.addObject("numPageMonth", nbPages);
		// Ajout des urls pour aller sur les différentes pages du détails
		mv.addObject("mapUrlPages",
				getPagesUrls(idCompte, currentMonth.getYear(), currentMonth.getMonthOfYear(), nbPages, typeDetail));
		// Ajout du numéro de la page courrante dans le modèle
		mv.addObject("currentPage", page);
		// Ajout de l'url pour aller dans les différents mois de l'historique dans le modèle
		mv.addObject("mapNamesUrlsForMonths", getMonthUrls(locale, idCompte, typeDetail));
	}
}

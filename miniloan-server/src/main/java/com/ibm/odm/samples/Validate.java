/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5655-Y31 5724-X98 5724-Y15 5655-V82 
* Copyright IBM Corp. 1987, 2018. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package com.ibm.odm.samples;

import ilog.rules.res.model.IlrPath;
import ilog.rules.res.session.IlrJ2SESessionFactory;
import ilog.rules.res.session.IlrSessionCreationException;
import ilog.rules.res.session.IlrSessionException;
import ilog.rules.res.session.IlrSessionFactory;
import ilog.rules.res.session.IlrSessionRequest;
import ilog.rules.res.session.IlrSessionResponse;
import ilog.rules.res.session.IlrStatelessSession;
import ilog.rules.res.session.ruleset.IlrExecutionEvent;
import ilog.rules.res.session.ruleset.IlrExecutionTrace;
import ilog.rules.res.session.ruleset.IlrRuleEvent;
import ilog.rules.res.session.ruleset.IlrRuleInformation;
import ilog.rules.res.session.ruleset.IlrTaskEvent;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import miniloan.Borrower;
import miniloan.Loan;



/**
 * Servlet implementation class Validate
 */
@WebServlet("/validate")
public class Validate extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Validate() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ")
				.append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		JsonReader jsonReader = Json.createReader(request.getInputStream());
		JsonObject json = jsonReader.readObject();
		jsonReader.close();
		log(json.toString());


		String result = validate(json);
		response.setContentType("application/json;charset=utf-8");
		PrintWriter pw = response.getWriter();
		pw.write(result);
		pw.flush();
		pw.close();

	}

	public String validate(JsonObject json) {
		try {
			Loan loan = new Loan(json.getInt("amount"),
								 json.getInt("duration"), 
								 json.getJsonNumber("yearly-interest-rate").doubleValue());

			Borrower borrower = new Borrower(json.getString("name"),
											 json.getInt("credit-score"), 
											 json.getInt("yearly-income"));

			String result = "";
			if ((boolean) json.getBoolean("use-odm")) {
				result = validateWithJRules(loan, borrower);
			} else {
				result = validateWithJava(loan, borrower);
			}

			System.out.println(result);
			return result;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return errorToJSON(e);
		}

	}

	// ODM Validation Part
	IlrSessionFactory sessionFactory = null;
	public String validateWithJRules(Loan loan, Borrower borrower) {
		String ss = "Empty";
		try {
			// get a rulesession
			if (sessionFactory == null) {
				sessionFactory = new IlrJ2SESessionFactory();
				ss = sessionFactory.toString();
			}

			// Create a session request object
			IlrSessionRequest sessionRequest = sessionFactory.createRequest();
			String ruleappName = Messages.getString("ruleappName");
			String rulesetName = Messages.getString("rulesetName");
			String rulesetPath = "/" + ruleappName + "/" + rulesetName;
			System.out.println("rulesetPath " + rulesetPath);
			sessionRequest.setForceUptodate(true);
			sessionRequest.setRulesetPath(IlrPath.parsePath(rulesetPath));
			// Enable trace to retrieve info on executed rules
			sessionRequest.setTraceEnabled(true);
			sessionRequest.getTraceFilter().setInfoAllFilters(true);
			
			// Set the input parameters for the execution of the rules
			Map<String, Object> inputParameters = sessionRequest.getInputParameters();
			inputParameters.put("loan", loan);
			inputParameters.put("borrower", borrower);

			IlrStatelessSession session = sessionFactory.createStatelessSession();

			// execute and get the response for this request
			IlrSessionResponse response = session.execute(sessionRequest);

			IlrExecutionTrace sessionTrace = response.getRulesetExecutionTrace();

			List<String> rulesFired = new ArrayList<String>();
			if (sessionTrace != null) {
				List<String> rules = getRulesExecuted(sessionTrace);
				rulesFired.addAll(rules);
			}

			long rulesNumber = sessionTrace.getTotalRulesFired();
			
			// including output parameters state
			loan = (Loan) response.getOutputParameters().get("loan");

			Collection<String> loanMessages = loan.getMessages();
			List<String> messages = new ArrayList<String>(loanMessages);
			long loanYearlyRepayment = loan.getYearlyRepayment();
			
			if (loan.isApproved()) {
				messages.add(0, "Your loan is approved with a yearly repayment of " + loanYearlyRepayment);
			} else {
				messages.add(0, "Your loan is rejected");
			}

			JsonObject value = Json.createObjectBuilder()
			.add("approved",loan.isApproved())
			.add("rulesNumber", rulesNumber)
			.add("messages", createJsonArrayFromList(messages, "line"))
			.add("info", createJsonArrayFromList(rulesFired, "line"))
			.build();

			return value.toString();

		} catch (IlrSessionCreationException e) {
			e.printStackTrace();
			// if no rulesession available, return with error
			return errorToJSON(e);
//			return ss;
		} catch (IlrSessionException e) {
			e.printStackTrace();
			// if rulesession fails, return with error
			return errorToJSON(e);
//			return ss;
		} catch (Exception e) {
			e.printStackTrace();
			return errorToJSON(e);
//			return ss;
		}
	}

	private String errorToJSON(Exception e) {
		JsonObject value = Json.createObjectBuilder()
		.add("error",true)
		.add("text", e.toString())
//		.add("text", "BOOOOOOMM")
		.build();
		return value.toString();
	}

	private JsonArray createJsonArrayFromList(Collection<String> coll, String keyName) {
		JsonArray value = null;
		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
	    for( String string : coll) {
	    	jsonArrayBuilder.add(Json.createObjectBuilder()
	    			.add(keyName, string));
	    }
	    value = jsonArrayBuilder.build();
	    return value;
	}


	// Java Validation Part
	public String validateWithJava(Loan loan, Borrower borrower) {
		checkMaximumAmount(loan, borrower);
		checkRepaymentAndScore(loan, borrower);
		checkMinimumIncome(loan, borrower);
		checkCreditScore(loan, borrower);
		List<String> messages = new ArrayList<String>(loan.getMessages());
		
		if (loan.isApproved()) {
			messages.add(0, "Your loan is approved with a yearly repayment of " + loan.getYearlyRepayment());
		} else {
			messages.add(0, "Your loan is rejected");
		}

		JsonObject value = Json.createObjectBuilder()
		.add("approved",loan.isApproved())
		.add("messages", createJsonArrayFromList(messages, "line"))
		.build();

		return value.toString();

	}

	/**
	 * check Repayment And Score
	 */
	private void checkRepaymentAndScore(Loan loan, Borrower borrower) {
		if (borrower.getYearlyIncome() > 0) {
			int val = loan.getYearlyRepayment() * 100
					/ borrower.getYearlyIncome();
			if ((val >= 0) && (val < 30) && (borrower.getCreditScore() >= 0)
					&& (borrower.getCreditScore() < 200)) {
				loan.addToMessages(Messages
						.getString("debttoincometoohighcomparedtocreditscore"));
				loan.reject();
			}
			if ((val >= 30) && (val < 45) && (borrower.getCreditScore() >= 0)
					&& (borrower.getCreditScore() < 400)) {
				loan.addToMessages(Messages
						.getString("debttoincometoohighcomparedtocreditscore"));
				loan.reject();
			}
			if ((val >= 45) && (val < 50) && (borrower.getCreditScore() >= 0)
					&& (borrower.getCreditScore() < 600)) {
				loan.addToMessages(Messages
						.getString("debttoincometoohighcomparedtocreditscore"));
				loan.reject();
			}
			if ((val >= 50) && (borrower.getCreditScore() >= 0)
					&& (borrower.getCreditScore() < 800)) {
				loan.addToMessages(Messages
						.getString("debttoincometoohighcomparedtocreditscore"));
				loan.reject();
			}
		}
	}

	/**
	 * Check Minimum Income
	 */
	private void checkMinimumIncome(Loan loan, Borrower borrower) {
		if (loan.getYearlyRepayment() > (borrower.getYearlyIncome() * 0.3d)) {
			loan.addToMessages(Messages.getString("toobigdebttoincomeratio"));
			loan.reject();
		}
	}

	/**
	 * Check Credit Score
	 */
	private void checkCreditScore(Loan loan, Borrower borrower) {
		if (borrower.getCreditScore() < 200) {
			loan.addToMessages(Messages.getString("creditscorebelow200"));
			loan.reject();
		}
	}

	/**
	 * Check Maximum Amount
	 */
	private void checkMaximumAmount(Loan loan, Borrower borrower) {
		if (loan.getAmount() > 1000000) {
			loan.addToMessages(Messages.getString("theloancannotexceed1000000"));
			loan.reject();
		}
	}

	/**
	 *
	 * @param trace
	 * @return the list of rules executed
	 */
	public static List<String> getRulesExecuted(IlrExecutionTrace trace) {
		List<String> firedRuleBusinessNames = new ArrayList<String>();
		Map<String, IlrRuleInformation> allRules = trace.getRules();
		List<IlrExecutionEvent> executionEvents = trace.getExecutionEvents();
		if (executionEvents != null && allRules != null) {
			String taskName = null;
			addRuleFiredBusinessNames(taskName, trace, executionEvents,firedRuleBusinessNames);
		}
		return firedRuleBusinessNames;
	}

	/**
	 * @param trace
	 * @param executionEvents
	 * @param firedRuleBusinessNames
	 */
	protected static void addRuleFiredBusinessNames(String taskName,
			IlrExecutionTrace trace, List<IlrExecutionEvent> executionEvents,
			List<String> firedRuleBusinessNames) {
		Map<String, IlrRuleInformation> allRules = trace.getRules();
		if (executionEvents != null && allRules != null) {
			for (IlrExecutionEvent event : executionEvents) {
				if (event instanceof IlrRuleEvent) {
					String ruleName = allRules.get(event.getName())
							.getBusinessName();
					firedRuleBusinessNames.add(formatTrace(ruleName, taskName));
				} else {
					List<IlrExecutionEvent> subEvents = ((IlrTaskEvent) event)
							.getSubExecutionEvents();
					IlrTaskEvent taskevent = (IlrTaskEvent) event;
					addRuleFiredBusinessNames(taskevent.getName(), trace,
							subEvents, firedRuleBusinessNames);
				}
			}
		}
	}
	
	protected static String escapeString(String str) {
		return str.replaceAll("\"", "\\\\\"").replaceAll("\n", "");
	}

	protected static String formatTrace(String ruleName, String taskName) {
		String format = Messages.getString("messagefiredinruletask");
		Object[] arguments = { ruleName, taskName };
		return MessageFormat.format(format, arguments);
	}

}

package com.example.sqlviarest;

/**
 * POJO to map JSON results from REST Enabled SQL Service.
 *
 * @author Loïc Lefèvre
 */
public class RESTEnabledSQLServiceResponse {

	private SQLServiceResponseEnv env;
	private SQLServiceResponseItem[] items;

	public RESTEnabledSQLServiceResponse() {
	}

	public SQLServiceResponseItem[] getItems() {
		return items;
	}

	public void setItems(SQLServiceResponseItem[] items) {
		this.items = items;
	}

	static class SQLServiceResponseEnv {
		private String defaultTimeZone;

		public SQLServiceResponseEnv() {
		}

		public String getDefaultTimeZone() {
			return defaultTimeZone;
		}

		public void setDefaultTimeZone(String defaultTimeZone) {
			this.defaultTimeZone = defaultTimeZone;
		}
	}

	static class SQLServiceResponseItem {
		private int statementId;
		private String statementType;
		private String statementText;
		private String[] response;
		private int errorCode;
		private int errorLine;
		private int errorColumn;
		private String errorDetails;
		private long result;

		public SQLServiceResponseItem() {
		}

		public int getStatementId() {
			return statementId;
		}

		public void setStatementId(int statementId) {
			this.statementId = statementId;
		}

		public String getStatementType() {
			return statementType;
		}

		public void setStatementType(String statementType) {
			this.statementType = statementType;
		}

		public String getStatementText() {
			return statementText;
		}

		public void setStatementText(String statementText) {
			this.statementText = statementText;
		}

		public String[] getResponse() {
			return response;
		}

		public void setResponse(String[] response) {
			this.response = response;
		}

		public int getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(int errorCode) {
			this.errorCode = errorCode;
		}

		public int getErrorLine() {
			return errorLine;
		}

		public void setErrorLine(int errorLine) {
			this.errorLine = errorLine;
		}

		public int getErrorColumn() {
			return errorColumn;
		}

		public void setErrorColumn(int errorColumn) {
			this.errorColumn = errorColumn;
		}

		public String getErrorDetails() {
			return errorDetails;
		}

		public void setErrorDetails(String errorDetails) {
			this.errorDetails = errorDetails;
		}

		public long getResult() {
			return result;
		}

		public void setResult(long result) {
			this.result = result;
		}
	}
}

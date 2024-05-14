package am.ik.translation.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueEvent(String action, Issue issue, Repository repository) {
	public record Issue(int number, String title) {

	}

	public record Repository(@JsonProperty("full_name") String fullName) {

	}
}

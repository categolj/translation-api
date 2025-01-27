package am.ik.translation.config;

import am.ik.translation.entry.Author;
import am.ik.translation.entry.Category;
import am.ik.translation.entry.Entry;
import am.ik.translation.entry.FrontMatter;
import am.ik.translation.entry.Tag;
import am.ik.translation.github.Commit;
import am.ik.translation.github.Committer;
import am.ik.translation.github.CreateBranchRequest;
import am.ik.translation.github.CreateBranchResponse;
import am.ik.translation.github.CreateContentRequest;
import am.ik.translation.github.CreatePullRequest;
import am.ik.translation.github.CreatePullResponse;
import am.ik.translation.github.IssueEvent;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeConfig.NativeRuntimeHints.class)
public class NativeConfig {

	public static class NativeRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection()
				.registerType(Author.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(Category.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(Entry.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(FrontMatter.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(Tag.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(Commit.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(Committer.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(CreateBranchRequest.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(CreateBranchResponse.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(CreateContentRequest.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(CreatePullRequest.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(CreatePullResponse.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				.registerType(IssueEvent.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		}

	}

}

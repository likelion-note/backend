package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/** 포트폴리오 테스트용 데이터 */
@Component
public class PortfolioTestSupport {

    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired JournalRepository journalRepository;

    public User member(String sub) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(14).build());
    }

    public ProjectCategory project(String title) {
        return projectRepository.save(ProjectCategory.builder().title(title).kind(ProjectKind.TEAM).cohort(14).build());
    }

    /** 내용이 채워진 일지 (DRAFT). 상태는 테스트에서 필요하면 바꿈 */
    public Journal journal(User author, ProjectCategory project, String stuck) {
        return journalRepository.saveAndFlush(Journal.builder().author(author).project(project).part(Part.BACKEND)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(Scope.NOW)
                .raw(new JournalBody(stuck + " " + JournalSummaryTest.STUCK_30, "넘긴 방법", null, null)).build());
    }

    /** 정리 + 검토 요청까지 (PENDING) */
    public Journal submitted(User author, ProjectCategory project, String stuck) {
        Journal j = journal(author, project, stuck);
        j.applySummary("제목", j.getRaw(), List.of());
        j.submit(List.of());
        journalRepository.flush();
        return j;
    }
}

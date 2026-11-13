package com.likelion.notebook.journal;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/** 운영진 검토 테스트용 데이터 */
@Component
public class ReviewTestSupport {

    @Autowired UserRepository userRepository;
    @Autowired JournalRepository journalRepository;

    public User user(String sub, UserType type, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(type).cohort(cohort).build());
    }

    /** 정리 + 검토 요청까지 (PENDING) */
    public Journal pending(User author, Scope scope, String title) {
        Journal j = journalRepository.save(Journal.builder().author(author).part(Part.BACKEND)
                .activity(Activity.HACKATHON).timing(Timing.W1_2).scope(scope)
                .raw(new JournalBody(JournalSummaryTest.STUCK_30, "원문 넘긴 방법", null, null)).build());
        j.applySummary(title, new JournalBody("정리된 막힌 것", "정리된 방법", null, null), List.of(Tag.MEETING_RUN));
        j.submit(List.of(Tag.MEETING_RUN));
        journalRepository.flush();
        return j;
    }

    public Journal draft(User author) {
        return journalRepository.saveAndFlush(Journal.builder().author(author).build());
    }
}

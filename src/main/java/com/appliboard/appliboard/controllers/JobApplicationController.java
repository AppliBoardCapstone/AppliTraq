package com.appliboard.appliboard.controllers;

import com.appliboard.appliboard.models.JobApplication;
import com.appliboard.appliboard.models.Note;
import com.appliboard.appliboard.models.Timeline;
import com.appliboard.appliboard.models.User;
import com.appliboard.appliboard.repositories.JobApplicationRepository;
import com.appliboard.appliboard.repositories.NoteRepository;
import com.appliboard.appliboard.repositories.TimelineRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.Instant;
import java.util.List;
import java.util.Date;


@Controller
public class JobApplicationController {
    private final JobApplicationRepository jobApplicationDao;
    private final NoteRepository noteDao;
    private final TimelineRepository timelineDao;

    public JobApplicationController(JobApplicationRepository jobApplicationDao, NoteRepository noteDao, TimelineRepository timelineDao) {
        this.jobApplicationDao = jobApplicationDao;
        this.noteDao = noteDao;
        this.timelineDao = timelineDao;
    }

//    VIEW ALL JOBAPPS
    @GetMapping("/jobApplications")
    public String viewJobs(Model model) {
//  USED A CUSTOM METHOD FROM JOBAPPS REPOSITORY TO FIND JOB APPS BY USER ID
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<JobApplication> listOfJobs = jobApplicationDao.findJobApplicationsByUserId(currentUser.getId());
        model.addAttribute("jobs", listOfJobs);
//      I NEED A LIST OF THE TIMELINE STATUSES ON FROM THE JOB LIST AS AN ATTRIBUTE
        List<JobApplication> listOfJobsAt1 = new ArrayList<>();
        List<JobApplication> listOfJobsAt2 = new ArrayList<>();
        List<JobApplication> listOfJobsAt3 = new ArrayList<>();
        List<JobApplication> listOfJobsAt4 = new ArrayList<>();

        for (JobApplication job : listOfJobs) {
            List<Timeline> allStatuses = timelineDao.findTimelinesByJobApplications(job);

            
            int lastIndex = allStatuses.size() -1;
            System.out.println("Job ID: " + job.getId());
            for ( Timeline timeline :  timelineDao.findTimelinesByJobApplications(job)) {
//                listOfTimelines.add(timeline);
                System.out.println("timeline id: " + timeline.getTimeline_id());
                System.out.println("timeline get kanban status: " + timeline.getKanban_status());
                if (timeline.getKanban_status() == 1) {
                    listOfJobsAt1.add(job);
                } else if (timeline.getKanban_status() == 2) {
                    listOfJobsAt2.add(job);
                } else if (timeline.getKanban_status() == 3) {
                    listOfJobsAt3.add(job);
                } else if (timeline.getKanban_status() == 4) {
                    listOfJobsAt4.add(job);
                }
            }
        }
        model.addAttribute("jobs1", listOfJobsAt1);
        model.addAttribute("jobs2", listOfJobsAt2);
        model.addAttribute("jobs3", listOfJobsAt3);
        model.addAttribute("jobs4", listOfJobsAt4);
//        List<JobApplication> listOfJobsAt3 = jobApplicationDao.findJobsByKanbanStatus(3);
//        model.addAttribute("jobs3", listOfJobsAt3);
        model.addAttribute("note", new Note());
//        model.addAttribute("notes", new Note());
        return "jobApplications/index";
    }
//tested to see if it was necessary, but doesnt seem like it. leaving here for reference will delete later
//    @PostMapping("/jobApplications")
//    public String createNote(Model model, @ModelAttribute Note note, @DateTimeFormat(pattern = "yyyy-MM-dd HH-mm-ss") Date fromDate) {
//        note.setJobApplication(jobApplicationDao.findById(1));
//        note.setDate(fromDate);
//        noteDao.save(note);
//        return "redirect:/notes/index";
//    }

//    SHOW SINGLE JOBAPP
    @GetMapping("/jobApplications/{id}")
    public String JobById(@PathVariable long id, Model model) {
        JobApplication jobApp = jobApplicationDao.getById(id);
        boolean isJobOwner = false;
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() != "anonymousUser") {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            isJobOwner = currentUser.getId() == jobApp.getUser().getId();
        }
        model.addAttribute("isJobOwner", isJobOwner);
        model.addAttribute("notes", noteDao.findNotesByJobApplicationId(id));
        model.addAttribute("jobApp", jobApp);
        model.addAttribute("note", new Note());

        return "jobApplications/show";
    }

//    CREATE JOBAPP
    @GetMapping("/jobApplications/create")
    public String showCreateForm(Model model) {
        model.addAttribute("job", new JobApplication());
        return "jobApplications/create";
    }

    @PostMapping("/jobApplications/create")
    public String create(@ModelAttribute JobApplication jobApp) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        jobApp.setUser(user);
        Timeline timeline = new Timeline(jobApp, Date.from(Instant.now()), 1);
        List<Timeline> newListOfTimelineStatus = new ArrayList<>();
        newListOfTimelineStatus.add(timeline);
        System.out.println("this should be id of timeline item: " + newListOfTimelineStatus.get(0).getTimeline_id());
        jobApp.setTimeline(newListOfTimelineStatus);
        System.out.println(newListOfTimelineStatus);
//        List<Timeline> newListOfTimelineStatus = new ArrayList<>();
//        newListOfTimelineStatus.add(timeline);
//        System.out.println("this should be id of timeline item: " + newListOfTimelineStatus.get(0).getTimeline_id());
//        jobApp.setTimeline(newListOfTimelineStatus);
//        System.out.println(newListOfTimelineStatus);
        jobApplicationDao.save(jobApp);
        timelineDao.save(timeline);

        return "redirect:/jobApplications/";
    }

//    EDIT JOBAPP
    @GetMapping("/jobApplications/{id}/edit")
    public String showEditForm(@PathVariable long id, Model model) {
        model.addAttribute("jobApp", jobApplicationDao.findById(id));
        return "jobApplications/edit";
    }

    @PostMapping("/jobApplications/{id}/edit")
    public String editJobApp(@PathVariable long id, @ModelAttribute JobApplication jobApp) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        jobApp.setUser(currentUser);
        jobApp.setId(id);
        jobApplicationDao.save(jobApp);
        return "redirect:/jobApplications/" + id;
    }

//    DELETE JOBAPP
    @PostMapping("/jobApplications/{id}/delete")
    public String deleteJob(@PathVariable long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        JobApplication jobApp = jobApplicationDao.getById(id);
        if (currentUser.getId() == jobApp.getUser().getId()) {
            for (Timeline timeline: timelineDao.findAll()) {
                if (timeline.getJobApplications().getId() == jobApp.getId()) {
                    timelineDao.delete(timeline);
                }
            }
            jobApplicationDao.delete(jobApp);
        }
        return "redirect:/jobApplications";
    }


//  UPDATE KANBAN TO STATUS
    @PostMapping("/jobApplications/kanban/update")
    public String updateKanbanStatus(@RequestParam(name = "jobId") List <Long> jobIds) {
        System.out.println(jobIds);
        int lastIndex = jobIds.size() - 1;
        System.out.println(jobIds.get(lastIndex));
        JobApplication jobApp = jobApplicationDao.getById(jobIds.get(lastIndex));

        List<Timeline> listOfStatuses = timelineDao.findTimelinesByJobApplications(jobApp);
        int lastIndexStatus = listOfStatuses.size() -1;
        System.out.println("This is last kanban status: " + listOfStatuses.get(lastIndexStatus).getKanbanStatus());
        int lastStatus =  listOfStatuses.get(lastIndexStatus).getKanbanStatus();
        System.out.println("new status interger: " + (lastStatus + 1));

        if (lastStatus == 4){
            lastStatus = 3;
        }
        Timeline newTimeline = new Timeline (jobApp, Date.from(Instant.now()), (lastStatus + 1));
        timelineDao.save(newTimeline);
        System.out.println("This submit works");
        return "redirect:/jobApplications";
    }

}

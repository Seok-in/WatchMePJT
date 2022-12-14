package com.A108.Watchme.Service;

import com.A108.Watchme.DTO.group.getGroupList.GroupMemberDetailResDTO;
import com.A108.Watchme.utils.AuthUtil;
import com.A108.Watchme.DTO.*;
import com.A108.Watchme.DTO.group.*;
import com.A108.Watchme.DTO.group.getGroup.*;
import com.A108.Watchme.DTO.group.getGroupList.GroupListResDTO;
import com.A108.Watchme.DTO.group.getGroupList.SprintDTO;
import com.A108.Watchme.Exception.CustomException;
import com.A108.Watchme.Http.ApiResponse;
import com.A108.Watchme.Http.Code;
import com.A108.Watchme.Repository.*;
import com.A108.Watchme.VO.ENUM.*;
import com.A108.Watchme.VO.Entity.Category;
import com.A108.Watchme.VO.Entity.MemberGroup;
import com.A108.Watchme.VO.Entity.group.Group;
import com.A108.Watchme.VO.Entity.group.GroupCategory;
import com.A108.Watchme.VO.Entity.group.GroupInfo;
import com.A108.Watchme.VO.Entity.log.GroupApplyLog;
import com.A108.Watchme.VO.Entity.log.MemberRoomLog;
import com.A108.Watchme.VO.Entity.log.PenaltyLog;
import com.A108.Watchme.VO.Entity.member.Member;
import com.A108.Watchme.VO.Entity.sprint.Sprint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepos;
    private final CategoryRepository categoryRepos;
    private final GroupCategoryRepository groupCategoryRepos;
    private final GroupInfoRepository groupInfoRepos;
    private final GroupApplyLogRegistory galRepos;
    private final PenaltyLogRegistory plRepos;
    private final MemberRepository memberRepos;
    private final MemberGroupRepository memberGroupRepos;
    private final MRLRepository mrlRepos;
    private final SprintRepository sprintRepos;

    private final S3Uploader s3Uploader;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private AuthUtil authUtil;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat format2 = new SimpleDateFormat("HH:mm");

    public ApiResponse getGroupList(String ctgName, String keyword, Integer page, HttpServletRequest request) {
        ApiResponse result = new ApiResponse();

        // Status??? YES??? ?????? ??????
        List<Group> groupList;

        if (page == null) {
            page = 1;
        }
        PageRequest pageRequest = PageRequest.of(page - 1, 9);

        if (ctgName != null) {
            Category category = categoryRepos.findByName(CategoryList.valueOf(ctgName));

            if (keyword == null) {
                groupList = groupRepos.findAllByCategory_categoryAndStatus(category, Status.YES, pageRequest).stream().collect(Collectors.toList());

            } else {
                groupList = groupRepos.findAllByCategory_categoryAndStatusAndGroupNameContaining(category, Status.YES, keyword, pageRequest).stream().collect(Collectors.toList());
            }

        } else {
            if (keyword == null) {
                groupList = groupRepos.findAllByStatusOrderByViewDesc(Status.YES, pageRequest).stream().collect(Collectors.toList());

            } else {
                groupList = groupRepos.findAllByStatusAndGroupNameContaining(Status.YES, keyword, pageRequest).stream().collect(Collectors.toList());
            }

        }

        // ????????? ????????? ?????? ??????
        if (groupList.isEmpty()) {
            throw new CustomException(Code.C520);
        }

        // ????????? ????????? ?????? ??????
        // getGroupList(Res DTO) : ?????? ????????? ??????
        List<GroupListResDTO> getGroupList = new LinkedList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);

        for (Group g : groupList) {
            // sprint : ????????? ????????? sprint
            // TODO : ???????????? sprint??? ???????????? ???????????? null??? ?????????

            List<Sprint> sprint = g.getSprints().stream().filter(x -> x.getSprintInfo().getStartAt().after(cal.getTime())).collect(Collectors.toList());
            // sprint ??? ????????? ????????? ?????? : ????????? ????????? ?????? ????????? ????????? ?????? ?????????.
            Sprint currSprint;

            // ?????? DTO List??? ??????
            getGroupList.add(GroupListResDTO.builder()
                    .id(g.getId())
                    .name(g.getGroupName())
                    .description(g.getGroupInfo().getDescription())
                    .currMember(g.getGroupInfo().getCurrMember())
                    .maxMember(g.getGroupInfo().getMaxMember())
                    .ctg(g.getCategory().stream().map(x -> x.getCategory().getName().toString()).collect(Collectors.toList()))
                    .imgLink(g.getGroupInfo().getImageLink())
                    .createdAt(format.format(g.getCreatedAt()))
                    .secret(g.getSecret() == 1 ? true : false)
                    .view(g.getView())
                    // ?????? ???????????? sprint??? ?????????
                    .sprint(!sprint.isEmpty() ?
                            SprintDTO.builder()
                                    .name((currSprint = sprint.get(0)).getName())
                                    .description(currSprint.getSprintInfo().getDescription())
                                    .startAt(format.format(currSprint.getSprintInfo().getStartAt()))
                                    .endAt(format.format(currSprint.getSprintInfo().getEndAt()))
                                    .status(currSprint.getStatus().toString())
                                    .build() : null
                    )
                    .build()
            );
        }

        result.setResponseData("groups", getGroupList);


        result.setCode(200);
        result.setMessage("GETROOMS SUCCESS");

        return result;
    }

    public ApiResponse getGroup(Long groupId, String pwd) {
        ApiResponse result = new ApiResponse();

        // ?????? ???????????? ??????
        Group group = checkGroup(groupId);

        // ????????? group??? ??????
        if (group.getStatus() == Status.NO || group.getStatus() == Status.DELETE) {
            throw new CustomException(Code.C510);
        }

        // ?????? DTO ?????? ??? ?????? : GroupResDTO : ?????? ??????
        result.setResponseData("group", GroupResDTO.builder()
                .name(group.getGroupName())
                .description(group.getGroupInfo().getDescription())
                .currMember(group.getGroupInfo().getCurrMember())
                .maxMember(group.getGroupInfo().getMaxMember())
                .ctg(group.getCategory().stream().map(x -> x.getCategory().getName().toString()).collect(Collectors.toList()))
                .imgLink(group.getGroupInfo().getImageLink())
                .createAt(format.format(group.getCreatedAt()))
                .display(group.getSecret())
                .view(group.getView())
                .build());


        // sprintResDTOList : ?????? DTO List
        List<SprintResDTO> sprintResDTOList;

        List<Sprint> sprintList = sprintRepos.findAllByGroupId(groupId).stream().filter(x -> x.getStatus() == Status.ING).collect(Collectors.toList());

        if (!sprintList.isEmpty()) {
            sprintResDTOList = new LinkedList<>();

            for (Sprint sprint : sprintList) {
                // ??????????????? ???????????? ??????????????? ??? ????????????
                int sumTime = 0;
                Optional<Integer> sum = mrlRepos.getSprintData(sprint.getRoom().getId());
                if (sum.isPresent()) {
                    sumTime = sum.get();
                }

                // ????????? ?????? ????????? ??? ????????? ????????? ?????? ??? ?????? ?????????
                String nickName = sprint.getGroup().getLeader().getNickName();
                Integer kingTime = 0;
                Integer count = 0;

                Optional<MemberRoomLog> checkMrl = mrlRepos.findTopByRoomIdOrderByStudyTimeDesc(sprint.getRoom().getId());
                if (checkMrl.isPresent()) {
                    MemberRoomLog memberRoomLog = checkMrl.get();

                    nickName = memberRoomLog.getMember().getNickName();
                    kingTime = memberRoomLog.getStudyTime();
                    count = plRepos.countByMemberIdAndRoomId(memberRoomLog.getMember().getId(), sprint.getRoom().getId());
                }

                // ????????? ????????? ??? ????????? ???
                int sumPenalty = plRepos.countByRoomId(sprint.getRoom().getId());

                sprintResDTOList.add(new SprintResDTO().builder()
                        .sprintId(sprint.getId())
                        .sprintImg(sprint.getSprintInfo().getImg())
                        .name(sprint.getName())
                        .description(sprint.getSprintInfo().getDescription())
                        .goal(sprint.getSprintInfo().getGoal())
                        .mode(sprint.getRoom().getMode().toString())
                        .endAt(format.format(sprint.getSprintInfo().getEndAt()))
                        .fee(sprint.getSprintInfo().getFee() != null ? sprint.getSprintInfo().getFee() : 0)
                        .penaltyMoney(sprint.getSprintInfo().getPenaltyMoney() != null ? sprint.getSprintInfo().getPenaltyMoney() : 0)
                        .startAt(format.format(sprint.getSprintInfo().getStartAt()))
                        .routineEndAt(format2.format(sprint.getSprintInfo().getRoutineEndAt()))
                        .routineStartAt(format2.format(sprint.getSprintInfo().getRoutineStartAt()))
                        .status(sprint.getStatus().toString())
                        .kingName(nickName)
                        .kingPenalty(count)
                        .kingStudy(kingTime)
                        .studySum(sumTime)
                        .penaltySum(sumPenalty)
                        .build());
            }

            result.setResponseData("sprints", sprintResDTOList);
        } else {
            result.setResponseData("sprints", new LinkedList<>());
        }


        // leader
        Member leader = group.getLeader();

        result.setResponseData("leader", new GroupMemberResDTO().builder()
                .nickName(leader.getNickName())
                .imgLink(leader.getMemberInfo().getImageLink())
                .role(GroupRole.LEADER.ordinal())
                .build());


        // ????????? ??????
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ????????????
        if (((UserDetails) authentication.getPrincipal()).getUsername().equals("anonymousUser")) {
            // ????????? ????????? ??????
            if (group.getSecret() == 1) {
                throw new CustomException(Code.C501);
            }

            // myData(????????????)
            List<Integer> penalties = new ArrayList<>();
            for (int i = 0; i < Mode.values().length; i++) {
                penalties.add(0);
            }

            result.setResponseData("myData", MyDataResDTO.builder()
                    .role(GroupRole.ANONYMOUS.ordinal())
                    .penalty(penalties)
                    .assign(0)
                    .studyTime(0)
                    .build());

            // groupData(????????????)
            List<Long> roomIdList = group.getSprints().stream().map(x -> x.getRoom().getId()).collect(Collectors.toList());
            List<MemberRoomLog> groupRoomLogList = mrlRepos.findByRoomIdIn(roomIdList);
            // groupData.sumTime
            int sumTime = 0;
            for (MemberRoomLog mrl : groupRoomLogList) {
                sumTime += mrl.getStudyTime();
            }

            GroupDataResDTO groupDataResDTO = GroupDataResDTO.builder()
                    .sumTime(sumTime)
                    .build();

            result.setResponseData("groupData", groupDataResDTO);


            result.setCode(200);
            result.setMessage("GET GROUP SUCCESS");

        } else {
            // ?????????
            String currUserId = ((UserDetails) authentication.getPrincipal()).getUsername();

            Optional<Member> checkMember = memberRepos.findById(Long.parseLong(currUserId));
            List<Member> groupMembers = group.getMemberGroupList().stream().map(x -> x.getMember()).collect(Collectors.toList());

            // ????????? ?????? ??????
            if (checkMember.isPresent() && groupMembers.stream().anyMatch(x -> x.getEmail().equals(checkMember.get().getEmail()))) {
                // ????????? + ?????????
                Member currMember = checkMember.get();

                // members
                List<GroupMemberResDTO> members = new LinkedList<>();

                for (Member m : groupMembers) {
                    members.add(GroupMemberResDTO.builder()
                            .nickName(m.getNickName())
                            .imgLink(m.getMemberInfo().getImageLink())
                            .role(m.getEmail().equals(leader.getEmail()) ? GroupRole.LEADER.ordinal() : GroupRole.MEMBER.ordinal())
                            .build()
                    );
                }

                result.setResponseData("members", members);


                // myData
                // myData.studyTime
                List<Long> roomIdList = group.getSprints().stream().map(x -> x.getRoom().getId()).collect(Collectors.toList());

                List<MemberRoomLog> memberRoomLogList = mrlRepos.findByMemberIdAndRoomIdIn(currMember.getId(), roomIdList);
                int studyTime = 0;

                for (MemberRoomLog mrl :
                        memberRoomLogList) {
                    studyTime += mrl.getStudyTime();
                }

                // myData.penalty
                List<Integer> penalty = new ArrayList<>();

                List<PenaltyLog> penaltyLogList = plRepos.findAllByMemberIdAndRoomIn(currMember.getId(), group.getSprints().stream().map(x -> x.getRoom()).collect(Collectors.toList()));

                for (Mode mode : Mode.values()) {
                    penalty.add(mode.ordinal(), (int) (long) penaltyLogList.stream().filter(x -> x.getMode().ordinal() == mode.ordinal()).count());
                }

                // myData.joinDate
                MemberGroup currMemberGroup = memberGroupRepos.findByMemberIdAndGroupId(currMember.getId(), groupId).get();

                result.setResponseData("myData", MyDataResDTO.builder()
                        .role(currMember.getEmail().equals(leader.getEmail()) ? GroupRole.LEADER.ordinal() : GroupRole.MEMBER.ordinal())
                        .studyTime(studyTime)
                        .penalty(penalty)
                        .joinDate(format.format(currMemberGroup.getCreatedAt()))
                        .build()
                );

                //groupdata
                List<MemberRoomLog> groupRoomLogList = mrlRepos.findByRoomIdIn(roomIdList);
                GroupDataResDTO groupDataResDTO;

                int sumTime = 0;
                for (MemberRoomLog mrl :
                        groupRoomLogList) {
                    sumTime += mrl.getStudyTime();
                }

                if (group.getLeader().getId().equals(currMember.getId())) {
                    int assignee = (int) galRepos.countByGroupIdAndStatus(groupId, 0);
                    groupDataResDTO = GroupDataResDTO.builder()
                            .sumTime(sumTime)
                            .assignee(assignee)
                            .build();
                } else {
                    groupDataResDTO = GroupDataResDTO.builder()
                            .sumTime(sumTime)
                            .build();
                }

                result.setResponseData("groupData", groupDataResDTO);

                result.setCode(200);
                result.setMessage("GET GROUP SUCCESS");

            } else if (checkMember.isPresent()) {
                if (group.getSecret() == 1) {
                    throw new CustomException(Code.C501);
                }

                // myData(????????????)
                List<Integer> penalties = new ArrayList<>();
                for (int i = 0; i < Mode.values().length; i++) {
                    penalties.add(0);
                }
                Collections.fill(penalties, 0);

                Optional<GroupApplyLog> gal = galRepos.findByMemberIdAndGroupId(checkMember.get().getId(), groupId);

                result.setResponseData("myData", MyDataResDTO.builder()
                        .role(GroupRole.ANONYMOUS.ordinal())
                        .assign(gal.isPresent() ? gal.get().getStatus() == 2 ? 2 : 1 : 0)
                        .penalty(penalties)
                        .studyTime(0)
                        .build());


                // groupData(????????????)
                List<Long> roomIdList = group.getSprints().stream().map(x -> x.getRoom().getId()).collect(Collectors.toList());
                List<MemberRoomLog> groupRoomLogList = mrlRepos.findByRoomIdIn(roomIdList);
                // groupData.sumTime
                int sumTime = 0;
                for (MemberRoomLog mrl : groupRoomLogList) {
                    sumTime += mrl.getStudyTime();
                }

                GroupDataResDTO groupDataResDTO = GroupDataResDTO.builder()
                        .sumTime(sumTime)
                        .build();

                result.setResponseData("groupData", groupDataResDTO);

                result.setCode(200);
                result.setMessage("GET GROUP SUCCESS");

            }
        }
        // members
        List<GroupMemberResDTO> members = new LinkedList<>();

        List<Member> groupMembers = group.getMemberGroupList().stream().map(x -> x.getMember()).collect(Collectors.toList());

        for (Member m : groupMembers) {
            members.add(GroupMemberResDTO.builder()
                    .nickName(m.getNickName())
                    .imgLink(m.getMemberInfo().getImageLink())
                    .role(m.getEmail().equals(leader.getEmail()) ? GroupRole.LEADER.ordinal() : GroupRole.MEMBER.ordinal())
                    .build()
            );
        }

        result.setResponseData("members", members);

        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse createGroup(GroupCreateReqDTO groupCreateReqDTO, MultipartFile image, HttpServletRequest request) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        String url = "https://popoimages.s3.ap-northeast-2.amazonaws.com/WatchMe/groups.jpg";

        if (image != null) {
            try {
                url = s3Uploader.upload(image, "Watchme");
            } catch (Exception e) {
                throw new CustomException(Code.C512);
            }
        }

        if (groupCreateReqDTO.getCtg().size() == 0) {
            throw new CustomException(Code.C511);
        }

        try {
            // 1.group ?????? ??????
            Group newGroup = Group.builder()
                    .groupName(groupCreateReqDTO.getName())
                    .leader(currUser)
                    .createdAt(new Date())
                    .status(Status.YES)
                    .view(0)
                    // TODO : Front?????? ?????? ??????????????? ????????? ??????
                    .secret(groupCreateReqDTO.getSecret())
                    .build();


            GroupInfo newGroupInfo = GroupInfo.builder()
                    .group(newGroup)
                    .imageLink(url)
                    .description(groupCreateReqDTO.getDescription())
                    .currMember(1)
                    .maxMember(groupCreateReqDTO.getMaxMember())
                    .build();

            GroupInfo nGI = groupInfoRepos.save(newGroupInfo);


            // 2.MemberGroup
            MemberGroup newMemberGroup = MemberGroup.builder()
                    .group(nGI.getGroup())
                    .member(nGI.getGroup().getLeader())
                    .createdAt(new Date())
                    .groupRole(GroupRole.LEADER)
                    .build();


            // 3.GroupCategory
            List<GroupCategory> newGroupCategory = new LinkedList<>();

            for (String ctg :
                    groupCreateReqDTO.getCtg()) {
                newGroupCategory.add(GroupCategory.builder()
                        .category(categoryRepos.findByName(CategoryList.valueOf(ctg)))
                        .group(nGI.getGroup())
                        .build());
            }


            // 4.GroupApplyLog
            GroupApplyLog newGroupApplyLog = GroupApplyLog.builder()
                    .member(currUser)
                    .group(nGI.getGroup())
                    .apply_date(new Date())
                    .status(1)
                    .build();


            memberGroupRepos.save(newMemberGroup);
            groupCategoryRepos.saveAll(newGroupCategory);
            galRepos.save(newGroupApplyLog);

            result.setCode(200);
            result.setMessage("SUCCESS ADD&JOIN ROOM");
            result.setResponseData("groupId", nGI.getGroup().getId());
        } catch (Exception e) {
            throw new CustomException(Code.C500);
        }

        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse updateGroup(Long groupId, GroupUpdateReqDTO groupUpdateReqDTO, MultipartFile image, HttpServletRequest request) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);
        GroupInfo groupInfo = groupInfoRepos.findById(groupId).get();

        if (currUser.getId() != group.getLeader().getId()) {
            throw new CustomException(Code.C536);
        }

        String url = group.getGroupInfo().getImageLink();

        if (image != null) {
            try {
                url = s3Uploader.upload(image, "Watchme");
            } catch (Exception e) {
                throw new CustomException(Code.C512);
            }
        }

        // ?????? ???????????? ??????
        List<GroupCategory> groupCategoryList = groupCategoryRepos.findAllByGroupId(groupId);
        groupCategoryRepos.deleteAllInBatch(groupCategoryList);

        try {
            // ?????? ???????????? ????????? ??????
            List<GroupCategory> categoryList = new LinkedList<>();

            try {
                for (String ctg : groupUpdateReqDTO.getCtg()) {
                    categoryList.add(GroupCategory.builder()
                            .category(categoryRepos.findByName(CategoryList.valueOf(ctg)))
                            .group(group)
                            .build());
                }
            } catch (Exception e) {
                throw new CustomException(Code.C521);
            }

            // update
            group.setGroupName(groupUpdateReqDTO.getName());

            // TODO : display? secret?
            group.setSecret(groupUpdateReqDTO.getDisplay() == null ? 0 : groupUpdateReqDTO.getDisplay());

            groupInfo.setDescription(groupUpdateReqDTO.getDescription());
            groupInfo.setMaxMember(Integer.parseInt(groupUpdateReqDTO.getMaxMember()));

            groupRepos.save(group);
            groupInfoRepos.save(groupInfo);
            groupCategoryRepos.saveAll(categoryList);
        } catch (Exception e) {
            throw new CustomException(Code.C500);
        }

        result.setCode(200);
        result.setMessage("SUCCESS GROUP UPDATE");

        return result;
    }

    public ApiResponse deleteGroup(Long groupId) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);

        if (group.getLeader().getId() == currUserId) {
            // TODO : Status??? DELETE??? ????????? ??????? ?????? ?????? ?????? ??????!! ex.?????????????????? ????????? ???????
            group.setStatus(Status.DELETE);

            groupRepos.save(group);

            result.setCode(200);
            result.setMessage("GROUP DELETE SUCCESS");
        } else {
            throw new CustomException(Code.C536);
        }

        return result;
    }

    public ApiResponse getApplyList(Long groupId) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);

        List<Long> groupMemberId = group.getMemberGroupList().stream().map(x -> x.getMember().getId()).collect(Collectors.toList());

        // ???????????? ????????? Error
        if (!groupMemberId.contains(currUserId)) {
            throw new CustomException(Code.C548);
        }

        // members
        List<GroupMemberDetailResDTO> members = new LinkedList<>();

        List<Member> groupMembers = galRepos.findAllByGroupId(group.getId())
                .stream().filter(x -> x.getStatus() == 1 && x.getMember().getId() != currUserId).map(x -> x.getMember()).collect(Collectors.toList());

        for (Member m : groupMembers) {
            List<Integer> penalty = new LinkedList<>();

            List<PenaltyLog> penaltyLogList = plRepos.findAllByMemberId(m.getId());

            if (penaltyLogList.size() != 0) {
                for (Mode mode : Mode.values()) {
                    penalty.add(mode.ordinal(), (int) penaltyLogList.stream().filter(x -> x.getMode().ordinal() == mode.ordinal()).count());
                }
            } else {
                for (Mode mode : Mode.values()) {
                    penalty.add(mode.ordinal(), 0);
                }
            }
            members.add(GroupMemberDetailResDTO.builder()
                    .nickName(m.getNickName())
                    // TODO : imgLink??? null??? ?????? ????????? ??????..?
                    .imgLink(m.getMemberInfo().getImageLink())
                    .email(m.getEmail())
                    .studyTime(m.getMemberInfo().getStudyTime())
                    .penalty(penalty)
                    .build()
            );
        }

        result.setResponseData("members", members);


        // appliers
        if (currUser.getId() == group.getLeader().getId()) {
            // ?????? ?????????
            List<GroupApplyLog> applyLogs = galRepos.findAllByGroupId(groupId);

            applyLogs = applyLogs.stream().filter(x -> x.getStatus() == 0).collect(Collectors.toList());

            List<GroupApplyDTO> getApplies = new LinkedList<>();

            for (GroupApplyLog applyLog : applyLogs) {
                Member member = applyLog.getMember();

                //
                List<Integer> penalty = new LinkedList<>();

                List<PenaltyLog> penaltyLogList = plRepos.findAllByMemberId(member.getId());

                if (penaltyLogList.size() != 0) {
                    for (Mode mode : Mode.values()) {
                        penalty.add(mode.ordinal(), (int) penaltyLogList.stream().filter(x -> x.getMode().ordinal() == mode.ordinal()).count());
                    }
                } else {
                    for (Mode mode : Mode.values()) {
                        penalty.add(mode.ordinal(), 0);
                    }
                }

                //
                getApplies.add(new GroupApplyDTO().builder()
                        .email(member.getEmail())
                        .nickName(member.getNickName())
                        .imgLink(member.getMemberInfo().getImageLink())
                        .studyTime(member.getMemberInfo().getStudyTime())
                        .penalty(penalty)
                        .build()
                );
            }

            result.setResponseData("appliers", getApplies);
        } else {
            result.setResponseData("appliers", null);
        }

        result.setCode(200);
        result.setMessage("GROUP APPLY LIST SUCCESS");

        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse apply(Long groupId) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);


        Optional<GroupApplyLog> groupApplyLog = galRepos.findByMemberIdAndGroupId(currUserId, groupId);
        List<Long> groupMembers = group.getMemberGroupList().stream().map(x -> x.getMember().getId()).collect(Collectors.toList());

        // ?????? ????????? ??????
        if (groupMembers.contains(currUserId)) {
            throw new CustomException(Code.C572);
        }

        if (groupApplyLog.isEmpty()) {
            if (group.getGroupInfo().getMaxMember() == group.getGroupInfo().getCurrMember()) {
                throw new CustomException(Code.C568);
            }

            galRepos.save(GroupApplyLog.builder()
                    .member(currUser)
                    .group(group)
                    .apply_date(new Date())
                    .status(0)
                    .build()
            );

            result.setCode(200);
            result.setMessage("GROUP JOIN APPLY SUCCESS");
        } else {
            if (groupApplyLog.get().getStatus() == 2) {
                throw new CustomException(Code.C566);
            } else if (groupApplyLog.get().getStatus() == 0) {
                throw new CustomException(Code.C567);
            } else if (groupApplyLog.get().getStatus() == 3) {
                throw new CustomException(Code.C552);
            } else {
                throw new CustomException(Code.C500);
            }

        }

        return result;
    }

    public ApiResponse cancelApply(Long groupId) {
        ApiResponse result = new ApiResponse();

        Group group = checkGroup(groupId);

        Long currUserId = authUtil.memberAuth();
        Member member = memberRepos.findById(currUserId).get();

        Optional<GroupApplyLog> checkGroupApplyLog = galRepos.findByMemberIdAndGroupId(currUserId, groupId);

        if (checkGroupApplyLog.isPresent()) {
            // ???????????? ??????
            GroupApplyLog groupApplyLog = checkGroupApplyLog.get();

            if (groupApplyLog.getStatus() == 0) {
                galRepos.delete(groupApplyLog);

                result.setCode(200);
                result.setMessage("GROUP APPLY CANCLE SUCCESS");

            } else if (groupApplyLog.getStatus() == 1) {
                throw new CustomException(Code.C572);

            } else if (groupApplyLog.getStatus() == 2) {
                throw new CustomException(Code.C566);
            }

        } else {
            throw new CustomException(Code.C300);
        }

        return result;
    }

    @Transactional
    public ApiResponse acceptApply(Long groupId, AcceptApplyReqDTO acceptApplyReqDTO) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);

        if (currUser.getId() == group.getLeader().getId()) {
            Optional<Member> applier = memberRepos.findByNickName(acceptApplyReqDTO.getNickName());

            Optional<GroupApplyLog> groupApplyLog = galRepos.findByMemberIdAndGroupId(applier.get().getId(), groupId);

            if (groupApplyLog.isPresent()) {
                if (group.getGroupInfo().getMaxMember() == group.getGroupInfo().getCurrMember()) {
                    throw new CustomException(Code.C568);
                }

                groupApplyLog.get().setStatus(1);
                groupApplyLog.get().setUpdate_date(new Date());

                group.getGroupInfo().setCurrMember(group.getGroupInfo().getCurrMember() + 1);

                groupRepos.save(group);

                galRepos.save(groupApplyLog.get());

                memberGroupRepos.save(MemberGroup.builder()
                        .member(applier.get())
                        .group(group)
                        .createdAt(new Date())
                        .groupRole(GroupRole.MEMBER)
                        .build()
                );

                result.setCode(200);
                result.setMessage("GROUP APPLY ACCEPT SUCCESS");
            } else {
                throw new CustomException(Code.C300);
            }

        } else {
            throw new CustomException(Code.C536);
        }

        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse declineApply(Long groupId, DeclineApplyReqDTO declineApplyReqDTO) {
        ApiResponse result = new ApiResponse();

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        Group group = checkGroup(groupId);

        if (currUser.getId() == group.getLeader().getId()) {
            Member member;
            try {
                member = memberRepos.findByNickName(declineApplyReqDTO.getNickName()).get();
            } catch (Exception e) {
                throw new CustomException(Code.C504);
            }

            Optional<GroupApplyLog> checkgroupApplyLog = galRepos.findByMemberIdAndGroupId(member.getId(), groupId);

            if (checkgroupApplyLog.isPresent()) {
                GroupApplyLog groupApplyLog = checkgroupApplyLog.get();
                if (groupApplyLog.getStatus() == 1) {
                    throw new CustomException(Code.C572);

                } else if (groupApplyLog.getStatus() == 0) {
                    groupApplyLog.setStatus(2);
                    galRepos.save(groupApplyLog);

                    result.setCode(200);
                    result.setMessage("GROUP APPLY DECLINE SUCCESS");

                } else if (groupApplyLog.getStatus() == 2) {
                    throw new CustomException(Code.C300);

                } else {
                    throw new CustomException(Code.C500);
                }

            } else {
                throw new CustomException(Code.C300);
            }

        } else {
            throw new CustomException(Code.C536);
        }


        return result;
    }

    @Transactional
    public ApiResponse leaveGroup(Long groupId) {
        ApiResponse result = new ApiResponse();

        Optional<Group> checkGroup = groupRepos.findById(groupId);

        if (checkGroup.isPresent()) {
            Long currUserId = authUtil.memberAuth();
            Member currUser = memberRepos.findById(currUserId).get();

            Optional<GroupApplyLog> groupApplyLog = galRepos.findByMemberIdAndGroupId(currUserId, groupId);

            if (groupApplyLog.isPresent()) {
                if (groupApplyLog.get().getStatus() == 1) {
                    galRepos.delete(groupApplyLog.get());

                    Optional<MemberGroup> memberGroup = memberGroupRepos.findByMemberIdAndGroupId(currUserId, groupId);

                    memberGroupRepos.delete(memberGroup.get());

                    result.setCode(200);
                    result.setMessage("GROUP LEAVE SUCCESS");

                } else {
                    throw new CustomException(Code.C300);
                }

            } else {
                throw new CustomException(Code.C300);
            }

        } else {
            throw new CustomException(Code.C510);
        }

        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    public ApiResponse tossLeader(Long groupId, LeaderTossReqDTO leaderTossReqDTO) {
        ApiResponse result = new ApiResponse();

        Group group = checkGroup(groupId);

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        if (currUser.getId() == group.getLeader().getId()) {
            Member member;
            try {
                member = memberRepos.findByNickName(leaderTossReqDTO.getNickName()).get();
            } catch (Exception e) {
                throw new CustomException(Code.C504);
            }

            // ?????? ???????????? ????????? ???????????? ????????? ????????????.
            Optional<MemberGroup> leaderGroup = memberGroupRepos.findByGroupIdAndMemberId(groupId, currUserId);
            Optional<MemberGroup> memberGroup = memberGroupRepos.findByGroupIdAndMemberId(groupId, member.getId());

            if (memberGroup.isPresent() && leaderGroup.isPresent()) {
                group.setLeader(member);

                leaderGroup.get().setGroupRole(GroupRole.MEMBER);
                memberGroup.get().setGroupRole(GroupRole.LEADER);

                groupRepos.save(group);
                memberGroupRepos.save(memberGroup.get());
                memberGroupRepos.save(leaderGroup.get());

                result.setCode(200);
                result.setMessage("GROUP LEADER CHANGE SUCCESS");
            } else {
                throw new CustomException(Code.C300);
            }

        } else {
            throw new CustomException(Code.C536);
        }


        return result;
    }

    public ApiResponse kickGroup(Long groupId, GroupKickReqDTO groupKickReqDTO) {
        ApiResponse result = new ApiResponse();

        Optional<Group> checkGroup = groupRepos.findById(groupId);

        if (checkGroup.isPresent()) {
            Group group = checkGroup.get();
            Long currUserId = authUtil.memberAuth();

            if (currUserId == checkGroup.get().getLeader().getId()) {
                Member member;

                try {
                    member = memberRepos.findByNickName(groupKickReqDTO.getNickName()).get();
                } catch (Exception e) {
                    throw new CustomException(Code.C504);
                }
                if (member.getId() == group.getLeader().getId()) {
                    throw new CustomException(Code.C300);
                }

                Optional<GroupApplyLog> checkGroupApplyLog = galRepos.findByMemberIdAndGroupId(member.getId(), groupId);
                Optional<MemberGroup> memberGroup = memberGroupRepos.findByMemberIdAndGroupId(member.getId(), groupId);

                if (checkGroupApplyLog.isPresent() && memberGroup.isPresent()) {
                    GroupApplyLog groupApplyLog = checkGroupApplyLog.get();

                    group.getGroupInfo().setCurrMember(group.getGroupInfo().getCurrMember()-1);

                    groupApplyLog.setStatus(3);
                    groupApplyLog.setUpdate_date(new Date());

                    groupRepos.save(group);
                    galRepos.save(groupApplyLog);

                    memberGroupRepos.delete(memberGroup.get());

                    result.setCode(200);
                    result.setMessage("GROUP MEMBER KICK SUCCESS");
                } else {
                    throw new CustomException(Code.C520);
                }

            } else {
                throw new CustomException(Code.C536);
            }
        } else {
            throw new CustomException(Code.C510);
        }

        return result;
    }

    public ApiResponse updateForm(Long groupId, HttpServletRequest request) {
        ApiResponse result = new ApiResponse();

        Group group = checkGroup(groupId);

        Long currUserId = authUtil.memberAuth();
        Member currUser = memberRepos.findById(currUserId).get();

        if (currUser.getId() == group.getLeader().getId()) {

            List<String> ctg = group.getCategory().stream().map(x -> x.getCategory().getName().toString()).collect(Collectors.toList());

            UpdateFormResDTO updateFormResDTO = UpdateFormResDTO.builder()
                    .id(group.getId())
                    .name(group.getGroupName())
                    .description(group.getGroupInfo().getDescription())
                    .maxMember(group.getGroupInfo().getMaxMember())
                    .ctg(ctg)
                    .secret(group.getSecret() == 1 ? true : false)
                    .imgLink(group.getGroupInfo().getImageLink())
                    .build();

            result.setResponseData("group", updateFormResDTO);

            result.setCode(200);
            result.setMessage("UPDATE-FORM SUCCESS");
        } else {
            throw new CustomException(Code.C536);
        }

        return result;
    }

    public Group checkGroup(Long groupId) {
        Group group;

        try {
            group = groupRepos.findById(groupId).get();
        } catch (Exception e) {
            throw new CustomException(Code.C510);
        }

        return group;
    }

}

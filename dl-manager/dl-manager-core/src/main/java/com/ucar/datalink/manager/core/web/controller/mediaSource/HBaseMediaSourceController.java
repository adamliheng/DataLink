package com.ucar.datalink.manager.core.web.controller.mediaSource;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ucar.datalink.biz.service.MediaSourceService;
import com.ucar.datalink.biz.utils.AuditLogOperType;
import com.ucar.datalink.biz.utils.AuditLogUtils;
import com.ucar.datalink.common.errors.ValidationException;
import com.ucar.datalink.domain.media.MediaSourceInfo;
import com.ucar.datalink.domain.media.MediaSourceType;
import com.ucar.datalink.domain.media.parameter.hbase.HBaseMediaSrcParameter;
import com.ucar.datalink.manager.core.coordinator.ClusterState;
import com.ucar.datalink.manager.core.coordinator.GroupMetadataManager;
import com.ucar.datalink.manager.core.server.ServerContainer;
import com.ucar.datalink.manager.core.web.dto.mediaSource.HBaseMediaSourceView;
import com.ucar.datalink.manager.core.web.util.AuditLogInfoUtil;
import com.ucar.datalink.manager.core.web.util.Page;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by user on 2017/6/19.
 */

@Controller
@RequestMapping(value = "/hbase/")
public class HBaseMediaSourceController {

    private static final Logger logger = LoggerFactory.getLogger(HBaseMediaSourceController.class);

    @Autowired
    MediaSourceService mediaSourceService;

    @RequestMapping(value = "/hbaseList")
    public ModelAndView hbaseList() {
        ModelAndView mav = new ModelAndView("hbaseMediaSource/list");
        return mav;
    }

    @RequestMapping(value = "/initHBase")
    @ResponseBody
    public Page<HBaseMediaSourceView> initHBase(@RequestBody Map<String, String> map) {
        Page<HBaseMediaSourceView> page = new Page<>(map);
        PageHelper.startPage(page.getPageNum(), page.getLength());

        Set<MediaSourceType> setMediaSource = new HashSet<>();
        boolean add = setMediaSource.add(MediaSourceType.HBASE);
        List<MediaSourceInfo> hbaseMediaSourceList = mediaSourceService.getListByType(setMediaSource);
        List<HBaseMediaSourceView> hbaseView = hbaseMediaSourceList.stream().map(i -> {
            HBaseMediaSourceView view = new HBaseMediaSourceView();
            view.setId(i.getId());
            view.setName(i.getName());
            view.setDesc(i.getDesc());
            view.setCreateTime(i.getCreateTime());
            view.setHbaseMediaSrcParameter(i.getParameterObj());
            view.getHbaseMediaSrcParameter().setZkMediaSourceId(((HBaseMediaSrcParameter) i.getParameterObj()).getZkMediaSourceId());
            MediaSourceInfo zk = mediaSourceService.getById(view.getHbaseMediaSrcParameter().getZkMediaSourceId());
            if (zk != null) {
                view.setZkMediaSourceName(zk.getName());
            }
            return view;
        }).collect(Collectors.toList());

        PageInfo<MediaSourceInfo> pageInfo = new PageInfo<MediaSourceInfo>(hbaseMediaSourceList);
        page.setDraw(page.getDraw());
        page.setAaData(hbaseView);
        page.setRecordsTotal((int) pageInfo.getTotal());
        page.setRecordsFiltered(page.getRecordsTotal());
        return page;
    }

    @RequestMapping(value = "/toAdd")
    public ModelAndView toAdd() {
        ModelAndView mav = new ModelAndView("hbaseMediaSource/add");
        mav.addObject("zkMediaSourceList", initZkList());
        return mav;
    }

    @ResponseBody
    @RequestMapping(value = "/doAdd")
    public String doAdd(@ModelAttribute("hbaseMediaSourceView") HBaseMediaSourceView hbaseMediaSourceView) {
        try {
            MediaSourceInfo mediaSourceInfo = buildHbaseMediaSourceInfo(hbaseMediaSourceView);
            Boolean isSuccess = mediaSourceService.insert(mediaSourceInfo);
            if (isSuccess) {
                AuditLogUtils.saveAuditLog(AuditLogInfoUtil.getAuditLogInfoFromMediaSourceInfo(mediaSourceInfo, "002002003", AuditLogOperType.insert.getValue()));
                return "success";
            }
        } catch (Exception e) {
            logger.error("Add HBase Media Source Error.", e);
            return e.getMessage();
        }
        return "fail";
    }

    @RequestMapping(value = "/toEdit")
    public ModelAndView toEdit(HttpServletRequest request) {
        String id = request.getParameter("id");
        ModelAndView mav = new ModelAndView("hbaseMediaSource/edit");
        MediaSourceInfo mediaSourceInfo = new MediaSourceInfo();
        if (StringUtils.isNotBlank(id)) {
            mediaSourceInfo = mediaSourceService.getById(Long.valueOf(id));
        }

        HBaseMediaSourceView view = new HBaseMediaSourceView();
        view.setId(mediaSourceInfo.getId());
        view.setName(mediaSourceInfo.getName());
        view.setDesc(mediaSourceInfo.getDesc());
        view.setCreateTime(mediaSourceInfo.getCreateTime());
        view.setHbaseMediaSrcParameter(mediaSourceInfo.getParameterObj());
        MediaSourceInfo zk = mediaSourceService.getById(view.getHbaseMediaSrcParameter().getZkMediaSourceId());
        if (zk != null) {
            view.setZkMediaSourceName(zk.getName());
        }

        mav.addObject("hbaseMediaSourceView", view);
        mav.addObject("zkMediaSourceList", initZkList());
        return mav;
    }

    @ResponseBody
    @RequestMapping(value = "/doEdit")
    public String doEdit(@ModelAttribute("hbaseMediaSourceView") HBaseMediaSourceView hbaseMediaSourceView) {
        try {
            if (hbaseMediaSourceView.getId() == null) {
                throw new RuntimeException("hbaseMediaSourceId is empty");
            }
            MediaSourceInfo mediaSourceInfo = buildHbaseMediaSourceInfo(hbaseMediaSourceView);
            mediaSourceInfo.setId(hbaseMediaSourceView.getId());
            Boolean isSuccess = mediaSourceService.update(mediaSourceInfo);
            toReloadDB(hbaseMediaSourceView.getId().toString());
            if (isSuccess) {
                AuditLogUtils.saveAuditLog(AuditLogInfoUtil.getAuditLogInfoFromMediaSourceInfo(mediaSourceInfo, "002002005", AuditLogOperType.update.getValue()));
                return "success";
            }
        } catch (Exception e) {
            logger.error("Update HBase Media Source Error.", e);
            return e.getMessage();
        }
        return "fail";
    }

    @ResponseBody
    @RequestMapping(value = "/doDelete")
    public String doDelete(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (StringUtils.isBlank(id)) {
            return "fail";
        }
        try {
            Long idLong = Long.valueOf(id);
            MediaSourceInfo mediaSourceInfo = mediaSourceService.getById(idLong);
            Boolean isSuccess = mediaSourceService.delete(idLong);
            if (isSuccess) {
                AuditLogUtils.saveAuditLog(AuditLogInfoUtil.getAuditLogInfoFromMediaSourceInfo(mediaSourceInfo
                        , "002002006", AuditLogOperType.delete.getValue()));
                return "success";
            }
        } catch (ValidationException e) {
            logger.error("Delete HBase Media Source Error.", e);
            return e.getMessage();
        }
        return "fail";
    }

    @ResponseBody
    @RequestMapping(value = "/checkHBase")
    public String checkHBase(HttpServletRequest request) {
        String id = request.getParameter("id");
        try {
            MediaSourceInfo mediaSourceInfo = new MediaSourceInfo();
            if (StringUtils.isNotBlank(id)) {
                mediaSourceInfo = mediaSourceService.getById(Long.valueOf(id));
            }
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @ResponseBody
    @RequestMapping(value = "/toReloadDB")
    public String toReloadDB(String mediaSourceId) {
        try {
            if (StringUtils.isBlank(mediaSourceId)) {
                throw new RuntimeException("mediaSourceId is empty");
            }
            GroupMetadataManager groupMetadataManager = ServerContainer.getInstance().getGroupCoordinator().getGroupManager();
            ClusterState clusterState = groupMetadataManager.getClusterState();
            if (clusterState == null) {
                return "success";
            }
            List<ClusterState.MemberData> memberDatas = clusterState.getAllMemberData();
            if (memberDatas == null || memberDatas.size() == 0) {
                return "success";
            }
            for (ClusterState.MemberData mem : memberDatas) {
                String url = "http://" + mem.getWorkerState().url() + "/flush/reloadHBase/" + mediaSourceId;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity request = new HttpEntity(null, headers);
                new RestTemplate().postForObject(url, request, Map.class);
            }
            MediaSourceInfo mediaSourceInfo = mediaSourceService.getById(Long.valueOf(mediaSourceId));
            AuditLogUtils.saveAuditLog(AuditLogInfoUtil.getAuditLogInfoFromMediaSourceInfo(mediaSourceInfo
                    , "002002008", AuditLogOperType.other.getValue()));
            return "success";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private MediaSourceInfo buildHbaseMediaSourceInfo(HBaseMediaSourceView hbaseMediaSourceView) {
        MediaSourceInfo hbaseMediaSourceInfo = new MediaSourceInfo();
        hbaseMediaSourceInfo.setName(hbaseMediaSourceView.getName());
        hbaseMediaSourceInfo.setDesc(hbaseMediaSourceView.getDesc());
        hbaseMediaSourceInfo.setType(MediaSourceType.HBASE);
        hbaseMediaSourceView.getHbaseMediaSrcParameter().setMediaSourceType(MediaSourceType.HBASE);
        hbaseMediaSourceInfo.setParameter(hbaseMediaSourceView.getHbaseMediaSrcParameter().toJsonString());
        return hbaseMediaSourceInfo;
    }

    private List<MediaSourceInfo> initZkList() {
        Set<MediaSourceType> setMediaSource = new HashSet<MediaSourceType>();
        setMediaSource.add(MediaSourceType.ZOOKEEPER);
        return mediaSourceService.getListByType(setMediaSource);
    }

}

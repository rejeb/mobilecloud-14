/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.http.*;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;

import javax.activation.MimeType;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.ResponseWrapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long,Video> videos;
    private VideoFileManager videoFileManager;
    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method= RequestMethod.GET)
    public @ResponseBody
    Collection<Video> getVideoList() {
        return videos.values();
    }
    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method= RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v) {
        if(v.getId()==0)
            v.setId(currentId.incrementAndGet());
        v.setDataUrl(getDataUrl(v.getId()));
        videos.put(v.getId(), v);
        return v;
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method= RequestMethod.POST)
    public  @ResponseBody VideoStatus setVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, @RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData) throws Exception {
        if(!videos.containsKey(id)) {
           throw new ResourceNotFoundException();
        }
        try {
            videoFileManager.saveVideoData(videos.get(id),videoData.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method= RequestMethod.GET)
    public HttpServletResponse getData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, HttpServletResponse response) throws IOException {
        if(!videos.containsKey(id)){
            throw new ResourceNotFoundException();
        }

        videoFileManager.copyVideoData(videos.get(id),response.getOutputStream());
        return response;
    }

    @PostConstruct
    private void init(){
        videos=new ConcurrentHashMap<Long,Video>();
        try {
            videoFileManager=VideoFileManager.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }
}

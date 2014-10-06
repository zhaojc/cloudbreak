package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.InviteRequest;
import com.sequenceiq.cloudbreak.controller.json.UpdateRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.facade.AdminUserFacade;
import com.sequenceiq.cloudbreak.security.CurrentUser;

@Controller
public class AdminUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    private AdminUserFacade adminUserFacade;

    @RequestMapping(method = RequestMethod.POST, value = "/users/invite")
    @ResponseBody
    public ResponseEntity<String> inviteUser(@CurrentUser User user, @RequestBody InviteRequest inviteRequest) {
        LOGGER.debug("Invite request received {}", inviteRequest);
        String inviteToken = adminUserFacade.inviteUser(user, inviteRequest);
        return new ResponseEntity<>(inviteToken, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/users/{userId}")
    @ResponseBody
    public ResponseEntity<UserJson> updateUser(@CurrentUser User admin, @RequestBody UpdateRequest updateRequest,
            @PathVariable("userId") Long userId) {
        LOGGER.debug("Update request received {}", updateRequest);
        UserJson usr = adminUserFacade.updateUser(admin, userId, updateRequest);
        return new ResponseEntity<>(usr, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @ResponseBody
    public ResponseEntity<List<UserJson>> accountUsers(@CurrentUser User admin) throws Exception {
        List<UserJson> accountUsers = adminUserFacade.accountUsers(admin);
        return new ResponseEntity<>(accountUsers, HttpStatus.OK);
    }

}

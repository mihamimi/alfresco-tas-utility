package org.alfresco.utility.data;

import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.dataprep.GroupService;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Data Preparation for Groups
 * 
 * @author Cristina Axinte
 */
@Service
@Scope(value = "prototype")
public class DataGroup extends TestData<DataGroup>
{
    @Autowired
    private GroupService groupService;

    /**
     * Creates a new random group.
     * 
     * @return
     */
    public GroupModel createRandomGroup()
    {
        String groupName = RandomData.getRandomName("Group");
        GroupModel groupModel = new GroupModel(groupName);
        
        groupModel = createGroup(groupModel);

        return groupModel;
    }
    
    /**
     * Creates a new group with admin user based on a group model
     * 
     * @return new created group
     */
    public GroupModel createGroup(GroupModel groupModel)
    {
        STEP(String.format("DATAPREP: Creating group %s with admin", groupModel.getDisplayName()));
        groupService.createGroup(getAdminUser().getUsername(), getAdminUser().getPassword(), groupModel.getDisplayName());

        return groupModel;
    }

    /**
     * Current user is added to the specified group
     * You can also use the {@link #usingUser(org.alfresco.utility.model.UserModel)}
     * method for defining a user to be added to the group
     * 
     * @return
     */
    public GroupModel addUserToGroup(GroupModel groupModel)
    {
        STEP(String.format("DATAPREP: Add user %s to group %s", getCurrentUser().getUsername(), groupModel.getDisplayName()));

        groupService.addUserToGroup(getAdminUser().getUsername(), getAdminUser().getPassword(), groupModel.getDisplayName(), 
                getCurrentUser().getUsername());
        
        return groupModel;
    }
    
    /**
     * Adds list of users to the specified group
     * 
     * @param groupModel
     * @param users
     * @return
     */
    public GroupModel addListOfUsersToGroup(GroupModel groupModel, UserModel...users)
    {
        for(UserModel userModel: users)
        {     
                usingUser(userModel).addUserToGroup(groupModel);
        }
        
        return groupModel;
    }

}

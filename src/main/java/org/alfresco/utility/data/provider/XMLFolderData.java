package org.alfresco.utility.data.provider;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.alfresco.utility.model.FolderModel;

/**
 * <folder name="f1" createdBy="user1">
 */
@XmlType(name = "folder")
public class XMLFolderData implements XMLDataItem
{
    private String name;
    private String createdBy;
    private List<XMLFileData> files = new ArrayList<XMLFileData>();
    private List<XMLFolderData> folders = new ArrayList<XMLFolderData>();
    private String parent;
    
    @XmlAttribute(name = "name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @XmlAttribute(name = "createdBy")
    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }
    
    @XmlElementWrapper
    @XmlElement(name = "file")
    public List<XMLFileData> getFiles()
    {                
        return files;
    }

    public void setFiles(List<XMLFileData> files)
    {
        this.files = files;
    }

    @Override
    public String toString()
    {
        StringBuilder info = new StringBuilder();
        info.append("site[name='")
            .append(getName()).append("',")
            .append("createdBy='")
            .append(getCreatedBy()).append("']");

        return info.toString();
    }
    
    @Override
    public FolderModel toModel()
    {
        FolderModel f = new FolderModel(getName());
        f.setCmisLocation(String.format("%s/%s", getParent(), getName()));
        return f;
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent(String parent)
    {
        this.parent = parent;
    }

    @XmlElementWrapper
    @XmlElement(name = "folder")
    public List<XMLFolderData> getFolders()
    {
        for(XMLFolderData f : folders)
        {
            f.setParent(toModel().getCmisLocation());
        }        
        return folders;
    }

    public void setFolders(List<XMLFolderData> folders)
    {
        this.folders = folders;
    }   
}
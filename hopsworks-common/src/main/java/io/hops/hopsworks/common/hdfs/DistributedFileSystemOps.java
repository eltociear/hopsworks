/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.hdfs;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.metadata.hdfs.entity.MetaStatus;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.security.UserGroupInformation;

public class DistributedFileSystemOps {

  private static final Logger logger = Logger.getLogger(DistributedFileSystemOps.class.getName());

  public static final String HOPSFS_SCHEME = "hopsfs";

  private final DistributedFileSystem dfs;
  private Configuration conf;
  private final String effectiveUser;

  public enum StoragePolicy {
    CLOUD("CLOUD"),
    SMALL_FILES("DB"),
    DEFAULT("HOT");

    private String policyName;

    StoragePolicy(String policyName) {
      this.policyName = policyName;
    }

    @Override
    public String toString() {
      return policyName;
    }
    
    public static StoragePolicy fromPolicy(String policyName) {
      switch(policyName) {
        case "CLOUD": return CLOUD;
        case "DB": return SMALL_FILES;
        case "HOT": return DEFAULT;
        default: throw new IllegalArgumentException("unknown policy:" + policyName);
      }
    }
  }

  /**
   * Returns a file system with username access.
   * <p>
   * @param ugi
   * @param conf
   */
  public DistributedFileSystemOps(UserGroupInformation ugi, Configuration
      conf, URI uri) {
    this.dfs = getDfs(ugi, conf, uri);
    this.conf = conf;
    effectiveUser = ugi.getUserName();
  }

  public DistributedFileSystemOps(UserGroupInformation ugi, Configuration conf) {
    this(ugi, conf, null);
  }
  
  private DistributedFileSystem getDfs(UserGroupInformation ugi,
          final Configuration conf, final URI uri) {
    FileSystem fs = null;
    try {
      fs = ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
        @Override
        public FileSystem run() throws IOException {
          if (null != uri) {
            return FileSystem.get(uri, conf);
          } else {
            return FileSystem.get(FileSystem.getDefaultUri(conf), conf);
          }
        }
      });
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Unable to initialize FileSystem", ex);
    }
    return (DistributedFileSystem) fs;
  }

  public DistributedFileSystem getFilesystem() {
    return dfs;
  }
  
  public String getEffectiveUser() {
    return effectiveUser;
  }

  /**
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(Path file) throws IOException {
    ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
    byte[] buffer = new byte[64 * 1024];
    int length;
    try (DataInputStream dataStream = dfs.open(file)) {
      while ((length = dataStream.read(buffer)) != -1) {
        outputBuffer.write(buffer, 0, length);
      }
    }
    return outputBuffer.toString("UTF-8");
  }

  /**
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(String file) throws IOException {
    Path path = new Path(file);
    return cat(path);
  }

  /**
   * Copy from HDFS to the local file system.
   * <p/>
   * @throws IOException
   */
  public void copyToLocal(String src, String dest) throws IOException {
    dfs.copyToLocalFile(new Path(src), new Path(dest));
  }

  /**
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdir(Path location, final FsPermission filePermission) throws IOException {
    return dfs.mkdir(location, filePermission);
  }

  /**
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param path
   * @return True if successful.
   * @throws java.io.IOException
   */
  public boolean mkdir(String path) throws IOException {
    Path location = new Path(path);
    return dfs.mkdir(location, FsPermission.getDefault());
  }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdirs(Path location, final FsPermission filePermission) throws IOException {
    return dfs.mkdirs(location, filePermission);
  }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @throws java.io.IOException
   */
  public void touchz(Path location) throws IOException {
    dfs.create(location).close();
  }

  /**
   * List the statuses of the files/directories in the given path if the path
   * is a directory.
   *
   * @param location
   * @return FileStatus[]
   * @throws IOException
   */
  public FileStatus[] listStatus(Path location) throws IOException {
    return dfs.listStatus(location);
  }

  /**
   * Returns the status information about the file.
   *
   * @param location
   * @return
   * @throws IOException
   */
  public FileStatus getFileStatus(Path location) throws IOException {
    return dfs.getFileStatus(location);
  }

  /**
   * Delete a file or directory from the file system.
   *
   * @param location The location of file or directory to be removed.
   * @param recursive If true, a directory will be removed with all its
   * children.
   * @return True if the operation is successful.
   * @throws IOException
   */
  public boolean rm(Path location, boolean recursive) throws IOException {
    logger.log(Level.INFO, "Deleting {0} as {1}", new Object[]{location.toString(), dfs.toString()});
    if (dfs.exists(location)) {
      return dfs.delete(location, recursive);
    }
    return true;
  }

  /**
   * Delete a file or directory from the file system.
   *
   * @param location The location of file or directory to be removed.
   * @param recursive If true, a directory will be removed with all its
   * children.
   * @return True if the operation is successful.
   * @throws IOException
   */
  public boolean rm(String location, boolean recursive) throws IOException {
    return rm(new Path(location), recursive);
  }

  /**
   * Copy a file from one file system to the other.
   * <p/>
   * @param deleteSource If true, the file at the source path will be deleted
   * after copying.
   * @param source
   * @param destination
   * @throws IOException
   */
  public void copyFromLocal(boolean deleteSource, Path source, Path destination) throws IOException {
    dfs.copyFromLocalFile(deleteSource, source, destination);
  }

  /**
   * Copy a file from the local path to the HDFS destination.
   * <p/>
   * @param deleteSource If true, deletes the source file after copying.
   * @param src
   * @param destination
   * @throws IOException
   * @throws IllegalArgumentException If the destination path contains an
   * invalid folder name.
   */
  public void copyToHDFSFromLocal(boolean deleteSource, String src, String destination) throws IOException {
    //Make sure the directories exist
    Path dirs = new Path(Utils.getDirectoryPart(destination));
    mkdirs(dirs, getParentPermission(dirs));
    //Actually copy to HDFS
    Path destp = new Path(destination);
    Path srcp = new Path(src);
    copyFromLocal(deleteSource, srcp, destp);
  }

  /**
   * Move a file in HDFS from one path to another.
   * <p/>
   * @param source
   * @param destination
   * @throws IOException
   */
  public void moveWithinHdfs(Path source, Path destination) throws IOException {
    dfs.rename(source, destination);
  }

  /**
   * Check if the path exists in HDFS.
   * <p/>
   * @param path
   * @return
   * @throws IOException
   */
  public boolean exists(String path) throws IOException {
    Path location = new Path(path);
    return dfs.exists(location);
  }
  
  public boolean exists(Path path) throws IOException {
    return dfs.exists(path);
  }

  /**
   * Copy a file within HDFS. Largely taken from Hadoop code.
   * <p/>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyInHdfs(Path src, Path dst) throws IOException {
    Path[] srcs = FileUtil.stat2Paths(dfs.globStatus(src), src);
    if (srcs.length > 1 && !dfs.isDirectory(dst)) {
      throw new IOException("When copying multiple files, "
              + "destination should be a directory.");
    }
    for (Path src1 : srcs) {
      FileUtil.copy(dfs, src1, dfs, dst, false, conf);
    }
  }

  /**
   * Creates a file and all parent dirs that does not exist and returns
   * an FSDataOutputStream
   *
   * @param path
   * @return FSDataOutputStream
   * @throws IOException
   */
  public FSDataOutputStream create(String path) throws IOException {
    Path dstPath = new Path(path);
    String dirs = Utils.getDirectoryPart(path);
    Path dirsPath = new Path(dirs);
    if (!exists(dirs)) {
      dfs.mkdirs(dirsPath);
    }
    return dfs.create(dstPath);
  }
  
  public FSDataOutputStream create(Path path) throws IOException {
    return create(path.toString());
  }
  
  /**
   * Creates a file and all parent dirs that does not exist and returns
   * an FSDataOutputStream ready for append
   *
   * @param path
   * @return FSDataOutputStream
   * @throws IOException
   */
  public FSDataOutputStream append(String path) throws IOException {
    return append(new Path(path));
  }
  
  public FSDataOutputStream append(Path path) throws IOException {
    if (!exists(path)) {
      return create(path);
    } else {
      return dfs.append(path);
    }
  }

  public void create(Path path, String content) throws IOException {
    try(FSDataOutputStream outputStream = dfs.create(path)) {
      outputStream.writeBytes(content);
      outputStream.flush();
    }
  }

  public void create(Path path, byte[] content) throws IOException {
    try(FSDataOutputStream outputStream = dfs.create(path)) {
      outputStream.write(content, 0, content.length);
      outputStream.flush();
    }
  }
  
  /**
   * Set permission for path.
   * <p>
   * @param path
   * @param permission
   * @throws IOException
   */
  public void setPermission(Path path, final FsPermission permission) throws IOException {
    dfs.setPermission(path, permission);
  }

  /**
   * Set acl for path.
   * <p>
   * @param path
   * @param aclEntries
   * @throws IOException
   */
  public void setPermission(Path path, final List<AclEntry> aclEntries) throws IOException {
    dfs.setAcl(path, aclEntries);
  }

  /**
   * Set permission for paths.
   * <p>
   * @param paths
   * @param permission
   * @throws IOException
   */
  public void setPermission(Set<Path> paths, final FsPermission permission) throws IOException {
    for (Path path : paths) {
      setPermission(path, permission);
    }
  }

  /**
   * Set owner for path.
   * <p>
   * @param path
   * @param username
   * @param groupname
   * @throws IOException
   */
  public void setOwner(Path path, String username, String groupname) throws IOException {
    dfs.setOwner(path, username, groupname);
  }

  public void setHdfsSpaceQuota(Path src, long diskspaceQuota) throws IOException {
    setHdfsQuotaBytes(src, HdfsConstants.QUOTA_RESET, diskspaceQuota);
  }

  /**
   *
   * @param src
   * @param numberOfFiles
   * @param diskspaceQuotaInBytes
   * @throws IOException
   */
  public void setHdfsQuotaBytes(Path src, long numberOfFiles, long diskspaceQuotaInBytes) throws IOException {
    dfs.setQuota(src, numberOfFiles, diskspaceQuotaInBytes);
  }

  public FSDataInputStream open(Path location) throws IOException {
    return this.dfs.open(location);
  }

  public FSDataInputStream open(String location) throws IOException {
    Path path = new Path(location);
    return this.dfs.open(path);
  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Returns the parents permission
   * <p>
   * @param path
   * @return
   * @throws IOException
   */
  public FsPermission getParentPermission(Path path) throws IOException {
    Path location = new Path(path.toUri());
    if (dfs.exists(location)) {
      location = location.getParent();
      return dfs.getFileStatus(location).getPermission();
    }
    while (!dfs.exists(location)) {
      location = location.getParent();
    }
    return dfs.getFileStatus(location).getPermission();
  }

  /**
   * Check if the path is a directory.
   * <p/>
   * @param path
   * @return
   */
  public boolean isDir(String path) {
    Path location = new Path(path);
    try {
      return dfs.isDirectory(location);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return false;
    }
  }
  
  /**
   * Set Meta ServiceStatus
   * <p/>
   * @throws IOException
   */
  public void setMetaStatus(Path path, Inode.MetaStatus status) throws IOException {
    switch(status) {
      case DISABLED:
        dfs.setMetaStatus(path, MetaStatus.DISABLED);
        break;
      case META_ENABLED:
        dfs.setMetaStatus(path, MetaStatus.META_ENABLED);
        break;
      case MIN_PROV_ENABLED:
        dfs.setMetaStatus(path, MetaStatus.MIN_PROV_ENABLED);
        break;
      case FULL_PROV_ENABLED:
        dfs.setMetaStatus(path, MetaStatus.FULL_PROV_ENABLED);
        break;
    }
  }
  
  /**
   * Set Provenance enabled flag on a given path
   * <p/>
   * @throws IOException
   */
  public void setMetaStatus(String location, Inode.MetaStatus status) throws IOException {
    Path path = new Path(location);
    setMetaStatus(path, status);
  }

  /**
   * Closes the distributed file system.
   */
  public void close() {
    try {
      dfs.close();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error while closing file system.", ex);
    }
  }

  public void addUser(String userName) throws IOException{
    dfs.addUser(userName);
  }
  
  public void removeUser(String userName) throws IOException{
    dfs.removeUser(userName);
  }
  
  public void addGroup(String groupName) throws IOException{
    dfs.addGroup(groupName);
  }
  
  public void removeGroup(String groupName) throws IOException{
    dfs.removeGroup(groupName);
  }
    
  public void addUserToGroup(String userName, String groupName) throws IOException{
    dfs.addUserToGroup(userName, groupName);
  }
  
  public void removeUserFromGroup(String userName, String groupName) throws IOException{
    dfs.removeUserFromGroup(userName, groupName);
  }

  public void setStoragePolicy(Path path, StoragePolicy storagePolicy) throws IOException {
    dfs.setStoragePolicy(path, storagePolicy.toString());
  }
  
  /**
   * Attach an extended attribute with a name to a file/directory in the given
   * path
   * @param path
   * @param name
   * @param value
   * @throws IOException
   */
  public void setXAttr(Path path, String name, byte[] value)
      throws IOException {
    dfs.setXAttr(path,  name, value);
  }
  
  /**
   * Remove an extended attribute using its name from a given file/directory.
   * @param path
   * @param name
   * @throws IOException
   */
  public void removeXAttr(Path path, String name) throws IOException {
    dfs.removeXAttr(path, name);
  }
  
  /**
   * Get the extended attribute using its name for a given file/directory.
   * @param path
   * @param name
   * @throws IOException
   */
  public byte[] getXAttr(Path path, String name) throws IOException {
    return dfs.getXAttr(path, name);
  }
  
  /**
   * Get all extended attribute for a given file/directory.
   * @param path
   * @throws IOException
   */
  public Map<String, byte[]> getXAttrs(Path path) throws IOException {
    return dfs.getXAttrs(path);
  }
}

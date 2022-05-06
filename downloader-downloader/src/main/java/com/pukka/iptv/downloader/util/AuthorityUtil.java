package com.pukka.iptv.downloader.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author zhengcl
 * @Date 2021/10/19
 * @description: 文件授权工具类
 * @Version 1.0
 */

@Slf4j
public class AuthorityUtil {
    // ftp用户文件存储的默认分组
    public static final String DEFAULT_FTP_USER_GROUP = "root";
    // 将要设置的权限：用户，组，以及三组读/写/执行的权限
    public static final PosixFilePermission[] PERMISSIONS_755 = {
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            //PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            //PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE,
    };

    private static void setFilePermissions(String pathStr, String owner, String group) {
        try {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Linux") || osName.startsWith("Unix") || osName.startsWith("Mac OS")) {
                // setLinuxPermission(pathStr, owner, group);
            } else if (osName.startsWith("Windows")) {
                setWindowsPermission(pathStr, owner, group);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    // 检查操作系统是否支持posix。
    // 一般像mac和linux都支持，经测试win7不支持
//        boolean supportPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
//        if (!supportPosix) {
//            System.out.println("Your OS does not support posix.");
//            return;
//        }
//}


    /**
     * @param deepChange  是否递归修改 文件夹 如果权限相同则不进行修改
     * @param pathStr     路径
     * @param owner       所有者
     * @param group       所属组
     * @param permissions 权限
     * @return void
     * @Description 修改文件夹的用户和组和权限
     * @author wz
     * @date 2021/11/24 18:24
     */
    public static void changePathPermission(boolean deepChange, String pathStr, String owner, String group, PosixFilePermission... permissions) {
        try {
            changeDirectoryPermission(deepChange, false, pathStr, owner, group, permissions);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    /**
     * @param deepChange  是否递归修改 文件夹和文件 如果权限相同则不进行修改
     * @param pathStr     路径
     * @param owner       所有者
     * @param group       所属组
     * @param permissions 权限
     * @return void
     * @Description 修改文件夹和内部的所有子文件夹以及文件
     * @author wz
     * @date 2021/11/24 18:25
     */
    public static void changePathAndFilePermission(boolean deepChange, String pathStr, String owner,
                                                   String group, PosixFilePermission... permissions) {
        try {
            changeDirectoryPermission(deepChange, true, pathStr, owner, group, permissions);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @param pathStr     路径
     * @param group       所属组
     * @param owner       所有者
     * @param permissions 权限
     * @return void
     * @Description 修改文件的权限 如果权限相同则不进行修改
     * @author wz
     * @date 2021/11/24 18:27
     */
    public static void changeFilePermission(String pathStr, String owner, String group, PosixFilePermission... permissions) {
        try {
            File file = new File(pathStr);
            if (file.isFile()) {
                changePermission(pathStr, owner, group, permissions);
            } else {
                log.info(pathStr + " not a file ");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void changeDirectoryPermission(boolean deepChange, boolean withFile, String pathStr,
                                                  String owner, String group,
                                                  PosixFilePermission... permissions) {
        File file = new File(pathStr);
        if (!withFile) {
            if (file.exists() && !file.isDirectory()) {
                log.warn("path 不是目录 {}", pathStr);
                return;
            }
        }
        //修改权限
        changePermission(pathStr, owner, group, permissions);
        if (deepChange) {
            if (file.isDirectory()) {
                Path path = Paths.get(pathStr);
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                    for (Path subPath : ds) {
                        try {
                            //递归修改所有文件夹下的内容
                            changeDirectoryPermission(deepChange, withFile, pathStr + File.separator + subPath.getFileName(), owner, group, permissions);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private static void changePermission(String pathStr, String owner, String group, PosixFilePermission... permissions) {
        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            log.info(pathStr + " not exists! ");
            return;
        }
        try {
            //新的权限组
            Set<PosixFilePermission> newPerm = new HashSet<>();
            Collections.addAll(newPerm, permissions);
            //原本信息
            Set<PosixFilePermission> oldPerm = Files.readAttributes(path, PosixFileAttributes.class).permissions();
            GroupPrincipal oldFileGroup = Files.readAttributes(path, PosixFileAttributes.class).group();
            UserPrincipal oldFileOwner = Files.readAttributes(path, PosixFileAttributes.class).owner();
            // log.info("permissions: {}, user: {}, group: {}", PosixFilePermissions.toString(oldPerm), oldFileOwner, oldFileGroup);
            //如果用户和用户组和权限 与传入的一致 不进行设置
            //log.info("oldFileGroup.getName={} oldFileOwner.getName()={}", oldFileGroup.getName(), oldFileOwner.getName());
            if (oldFileGroup.getName().equalsIgnoreCase(group)
                    && oldFileOwner.getName().equalsIgnoreCase(owner)
                    && isSamePermission(oldPerm, newPerm)) {
                //log.info("用户和用户组和权限完全与传入的一致 不进行设置");
                return;
            }
            chmod(path, newPerm);
            chown(path, owner, group);
            readTargetPermission(path);
        } catch (IOException e) {
            if (e instanceof UserPrincipalNotFoundException) {
                log.info("group '{}' or owner '{}' not exist%n", group, owner);
                return;
            }
            log.info("{} set permission failed", pathStr);
            log.error(e.getMessage());
        }
    }

    //判断两组权限是否完全一致
    private static boolean isSamePermission(Set<PosixFilePermission> oldPermission, Set<PosixFilePermission> nowPermission) {
        if (oldPermission.size() != nowPermission.size()) {
            return false;
        }
        for (PosixFilePermission p : oldPermission) {
            if (!nowPermission.contains(p)) {
                return false;
            }
        }
        for (PosixFilePermission n : nowPermission) {
            if (!oldPermission.contains(n)) {
                return false;
            }
        }
        return true;
    }

    // 设置permission，相当于linux命令chmod
    private static void chmod(Path path, Set<PosixFilePermission> permissions) throws IOException {
        Files.setPosixFilePermissions(path, permissions);
    }

    // 设置用户和组，相当于linux命令chown
    // 要保证用户和组存在，否则lookupService会抛UserPrincipalNotFoundException
    private static void chown(Path path, String owner, String group) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(group);
        UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(owner);
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        view.setGroup(groupPrincipal);
        view.setOwner(userPrincipal);
    }

    // print current permission of folder/file
    private static void readTargetPermission(Path path) throws IOException {
        Set<PosixFilePermission> filePermissions = Files.readAttributes(path, PosixFileAttributes.class).permissions();
        GroupPrincipal fileGroup = Files.readAttributes(path, PosixFileAttributes.class).group();
        UserPrincipal fileOwner = Files.readAttributes(path, PosixFileAttributes.class).owner();
        log.info("permissions: {}, user: {}, group: {}", PosixFilePermissions.toString(filePermissions), fileOwner, fileGroup);
    }

    //Windows系统
    public static void setWindowsPermission(String pathStr, String owner, String group) {
        Path path = Paths.get(pathStr);
        AclFileAttributeView aclView = Files.getFileAttributeView(path,
                AclFileAttributeView.class);
        if (aclView == null) {
            log.info("acl view  is not  supported.%n");
            return;
        }
        try {
            AclEntryPermission[] permissions = {
                    AclEntryPermission.READ_ACL,
                    AclEntryPermission.WRITE_DATA,
                    AclEntryPermission.READ_DATA,
                    AclEntryPermission.DELETE,
                    AclEntryPermission.EXECUTE,
                    AclEntryPermission.ADD_FILE,
                    AclEntryPermission.ADD_SUBDIRECTORY,
                    AclEntryPermission.APPEND_DATA,
                    AclEntryPermission.DELETE_CHILD,
                    AclEntryPermission.LIST_DIRECTORY,
                    AclEntryPermission.READ_ATTRIBUTES,
                    AclEntryPermission.READ_NAMED_ATTRS,
                    AclEntryPermission.SYNCHRONIZE,
                    AclEntryPermission.WRITE_ACL,
                    AclEntryPermission.WRITE_ATTRIBUTES,
                    AclEntryPermission.WRITE_NAMED_ATTRS,
                    AclEntryPermission.WRITE_OWNER
            };
            UserPrincipalLookupService userPrincipalLookupService = FileSystems.getDefault().getUserPrincipalLookupService();
            UserPrincipal userPrincipal = userPrincipalLookupService.lookupPrincipalByName(owner);
//            GroupPrincipal groupPrincipal = userPrincipalLookupService.lookupPrincipalByGroupName(group);
            // 设置permission，相当于linux命令chmod
            Set<AclEntryPermission> perms = new HashSet<>();
            Collections.addAll(perms, permissions);
            AclEntry.Builder builder = AclEntry.newBuilder();
            builder.setPrincipal(userPrincipal);
            builder.setType(AclEntryType.ALLOW);
            builder.setPermissions(perms);
            AclEntry newEntry = builder.build();
            List<AclEntry> aclEntries = aclView.getAcl();
            aclEntries.add(newEntry);
            aclView.setAcl(aclEntries);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}

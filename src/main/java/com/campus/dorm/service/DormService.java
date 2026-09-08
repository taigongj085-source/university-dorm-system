package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.DormBuilding;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.DormBuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DormService {

    private final DormBuildingMapper buildingMapper;
    private final DormRoomMapper roomMapper;
    private final DormBedMapper bedMapper;
    private final StudentMapper studentMapper;
    private final StatsCacheService statsCacheService;

    /* ---------- Building ---------- */
    public Page<DormBuilding> pageBuildings(String q, long page, long size) {
        LambdaQueryWrapper<DormBuilding> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(DormBuilding::getName, q).or().like(DormBuilding::getCode, q));
        }
        qw.orderByAsc(DormBuilding::getCode);
        return buildingMapper.selectPage(new Page<>(page, size), qw);
    }

    public List<DormBuilding> listBuildings() {
        return buildingMapper.selectList(new LambdaQueryWrapper<DormBuilding>()
                .eq(DormBuilding::getStatus, 1)
                .orderByAsc(DormBuilding::getCode));
    }

    public void saveBuilding(DormBuilding form) {
        if (StrUtil.isBlank(form.getName()) || StrUtil.isBlank(form.getCode())) {
            throw new BusinessException("楼栋名称和编号不能为空");
        }
        Long exists = buildingMapper.selectCount(new LambdaQueryWrapper<DormBuilding>()
                .eq(DormBuilding::getCode, form.getCode())
                .ne(form.getId() != null, DormBuilding::getId, form.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("楼栋编号已存在");
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        if (form.getGenderType() == null) {
            form.setGenderType(1);
        }
        if (form.getFloors() == null) {
            form.setFloors(6);
        }
        if (form.getId() == null) {
            buildingMapper.insert(form);
        } else {
            buildingMapper.updateById(form);
        }
        statsCacheService.clearCache();
    }

    public void deleteBuilding(Long id) {
        Long rooms = roomMapper.selectCount(new LambdaQueryWrapper<DormRoom>()
                .eq(DormRoom::getBuildingId, id));
        if (rooms != null && rooms > 0) {
            throw new BusinessException("楼栋下仍有房间，无法删除");
        }
        buildingMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    /* ---------- Room ---------- */
    public Page<DormRoom> pageRooms(Long buildingId, String q, long page, long size) {
        LambdaQueryWrapper<DormRoom> qw = new LambdaQueryWrapper<>();
        if (buildingId != null) {
            qw.eq(DormRoom::getBuildingId, buildingId);
        }
        if (StrUtil.isNotBlank(q)) {
            qw.like(DormRoom::getRoomNo, q);
        }
        qw.orderByAsc(DormRoom::getBuildingId).orderByAsc(DormRoom::getRoomNo);
        Page<DormRoom> result = roomMapper.selectPage(new Page<>(page, size), qw);
        for (DormRoom room : result.getRecords()) {
            fillRoomMeta(room);
        }
        return result;
    }

    public List<DormRoom> listRooms(Long buildingId) {
        LambdaQueryWrapper<DormRoom> qw = new LambdaQueryWrapper<DormRoom>()
                .eq(DormRoom::getStatus, 1);
        if (buildingId != null) {
            qw.eq(DormRoom::getBuildingId, buildingId);
        }
        qw.orderByAsc(DormRoom::getRoomNo);
        List<DormRoom> list = roomMapper.selectList(qw);
        list.forEach(this::fillRoomMeta);
        return list;
    }

    public DormRoom getRoom(Long id) {
        DormRoom room = roomMapper.selectById(id);
        if (room != null) {
            fillRoomMeta(room);
        }
        return room;
    }

    public void saveRoom(DormRoom form) {
        if (form.getBuildingId() == null || StrUtil.isBlank(form.getRoomNo())) {
            throw new BusinessException("请选择楼栋并填写房间号");
        }
        Long exists = roomMapper.selectCount(new LambdaQueryWrapper<DormRoom>()
                .eq(DormRoom::getBuildingId, form.getBuildingId())
                .eq(DormRoom::getRoomNo, form.getRoomNo())
                .ne(form.getId() != null, DormRoom::getId, form.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该楼栋下房间号已存在");
        }
        if (form.getBedCount() == null || form.getBedCount() <= 0) {
            form.setBedCount(4);
        }
        if (form.getFloorNo() == null) {
            form.setFloorNo(1);
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        boolean creating = form.getId() == null;
        if (creating) {
            roomMapper.insert(form);
            // 自动生成床位 A/B/C...
            String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H"};
            int count = Math.min(form.getBedCount(), labels.length);
            for (int i = 0; i < count; i++) {
                DormBed bed = new DormBed();
                bed.setRoomId(form.getId());
                bed.setBedNo(labels[i]);
                bed.setStatus(0);
                bedMapper.insert(bed);
            }
        } else {
            roomMapper.updateById(form);
        }
        statsCacheService.clearCache();
    }

    public void deleteRoom(Long id) {
        Long occupied = bedMapper.selectCount(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getRoomId, id)
                .eq(DormBed::getStatus, 1));
        if (occupied != null && occupied > 0) {
            throw new BusinessException("房间仍有入住床位，无法删除");
        }
        bedMapper.delete(new LambdaQueryWrapper<DormBed>().eq(DormBed::getRoomId, id));
        roomMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    private void fillRoomMeta(DormRoom room) {
        DormBuilding building = buildingMapper.selectById(room.getBuildingId());
        if (building != null) {
            room.setBuildingName(building.getName());
        }
        Long occupied = bedMapper.selectCount(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getRoomId, room.getId())
                .eq(DormBed::getStatus, 1));
        room.setOccupiedCount(occupied == null ? 0 : occupied.intValue());
    }

    /* ---------- Bed ---------- */
    public Page<DormBed> pageBeds(Long roomId, Long buildingId, Integer status, long page, long size) {
        LambdaQueryWrapper<DormBed> qw = new LambdaQueryWrapper<>();
        if (roomId != null) {
            qw.eq(DormBed::getRoomId, roomId);
        } else if (buildingId != null) {
            List<DormRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<DormRoom>()
                    .eq(DormRoom::getBuildingId, buildingId));
            if (rooms.isEmpty()) {
                return new Page<>(page, size);
            }
            qw.in(DormBed::getRoomId, rooms.stream().map(DormRoom::getId).toList());
        }
        if (status != null) {
            qw.eq(DormBed::getStatus, status);
        }
        qw.orderByAsc(DormBed::getRoomId).orderByAsc(DormBed::getBedNo);
        Page<DormBed> result = bedMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(this::fillBedMeta);
        return result;
    }

    public List<DormBed> listBedsByRoom(Long roomId) {
        List<DormBed> beds = bedMapper.selectList(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getRoomId, roomId)
                .orderByAsc(DormBed::getBedNo));
        beds.forEach(this::fillBedMeta);
        return beds;
    }

    public void saveBed(DormBed form) {
        if (form.getRoomId() == null || StrUtil.isBlank(form.getBedNo())) {
            throw new BusinessException("房间和床位号不能为空");
        }
        Long exists = bedMapper.selectCount(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getRoomId, form.getRoomId())
                .eq(DormBed::getBedNo, form.getBedNo())
                .ne(form.getId() != null, DormBed::getId, form.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该房间床位号已存在");
        }
        if (form.getStatus() == null) {
            form.setStatus(form.getStudentId() == null ? 0 : 1);
        }
        if (form.getId() == null) {
            bedMapper.insert(form);
        } else {
            bedMapper.updateById(form);
        }
        statsCacheService.clearCache();
    }

    @Transactional
    public void checkIn(Long bedId, Long studentId, LocalDate checkInDate) {
        DormBed bed = bedMapper.selectById(bedId);
        if (bed == null) {
            throw new BusinessException("床位不存在");
        }
        if (bed.getStatus() != null && bed.getStatus() == 1 && bed.getStudentId() != null) {
            throw new BusinessException("床位已有人入住");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        DormBed occupied = bedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, studentId)
                .eq(DormBed::getStatus, 1)
                .last("LIMIT 1"));
        if (occupied != null) {
            throw new BusinessException("该学生已入住其他床位，请先退宿");
        }
        DormBed upd = new DormBed();
        upd.setId(bedId);
        upd.setStudentId(studentId);
        upd.setCheckInDate(checkInDate == null ? LocalDate.now() : checkInDate);
        upd.setStatus(1);
        bedMapper.updateById(upd);
        statsCacheService.clearCache();
    }

    @Transactional
    public void checkOut(Long bedId) {
        DormBed bed = bedMapper.selectById(bedId);
        if (bed == null) {
            throw new BusinessException("床位不存在");
        }
        DormBed upd = new DormBed();
        upd.setId(bedId);
        upd.setStudentId(null);
        upd.setCheckInDate(null);
        upd.setStatus(0);
        bedMapper.updateById(upd);
        // MyBatis-Plus 默认忽略 null，需强制清空
        bedMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DormBed>()
                .eq(DormBed::getId, bedId)
                .set(DormBed::getStudentId, null)
                .set(DormBed::getCheckInDate, null)
                .set(DormBed::getStatus, 0));
        statsCacheService.clearCache();
    }

    public void deleteBed(Long id) {
        DormBed bed = bedMapper.selectById(id);
        if (bed != null && bed.getStatus() != null && bed.getStatus() == 1) {
            throw new BusinessException("已入住床位不可删除，请先退宿");
        }
        bedMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    private void fillBedMeta(DormBed bed) {
        DormRoom room = roomMapper.selectById(bed.getRoomId());
        if (room != null) {
            DormBuilding building = buildingMapper.selectById(room.getBuildingId());
            String bName = building == null ? "" : building.getName();
            bed.setRoomLabel(bName + " " + room.getRoomNo());
        }
        if (bed.getStudentId() != null) {
            Student student = studentMapper.selectById(bed.getStudentId());
            if (student != null) {
                bed.setStudentName(student.getName());
                bed.setStudentNo(student.getStudentNo());
            }
        }
    }

    /** 学生端：我的宿舍详情 */
    public Map<String, Object> myDormDetail(Long studentId) {
        Map<String, Object> data = new HashMap<>();
        DormBed myBed = bedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, studentId)
                .eq(DormBed::getStatus, 1)
                .last("LIMIT 1"));
        if (myBed == null) {
            data.put("assigned", false);
            return data;
        }
        fillBedMeta(myBed);
        DormRoom room = roomMapper.selectById(myBed.getRoomId());
        fillRoomMeta(room);
        DormBuilding building = buildingMapper.selectById(room.getBuildingId());
        List<DormBed> beds = listBedsByRoom(room.getId());
        data.put("assigned", true);
        data.put("bed", myBed);
        data.put("room", room);
        data.put("building", building);
        data.put("beds", beds);
        return data;
    }

    public String roomLabel(Long roomId) {
        if (roomId == null) {
            return "-";
        }
        DormRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            return "-";
        }
        DormBuilding building = buildingMapper.selectById(room.getBuildingId());
        return (building == null ? "" : building.getName()) + " " + room.getRoomNo();
    }
}

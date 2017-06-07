/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.lecture.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.TemporalType;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockRef;
import org.olat.modules.lecture.LectureBlockStatus;
import org.olat.modules.lecture.LectureRollCallStatus;
import org.olat.modules.lecture.model.LectureBlockImpl;
import org.olat.modules.lecture.model.LectureBlockToGroupImpl;
import org.olat.modules.lecture.model.LectureBlockWithTeachers;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 17 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class LectureBlockDAO {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;
	
	public LectureBlock createLectureBlock(RepositoryEntry entry) {
		LectureBlockImpl block = new LectureBlockImpl();
		block.setCreationDate(new Date());
		block.setLastModified(block.getCreationDate());
		block.setStatus(LectureBlockStatus.active);
		block.setRollCallStatus(LectureRollCallStatus.open);
		block.setEntry(entry);
		return block;
	}
	
	public LectureBlock update(LectureBlock block) {
		if(block.getKey() == null) {
			((LectureBlockImpl)block).setTeacherGroup(groupDao.createGroup());
			dbInstance.getCurrentEntityManager().persist(block);
		} else {
			block.setLastModified(new Date());
			block = dbInstance.getCurrentEntityManager().merge(block);
		}
		return block;
	}
	
	public LectureBlock loadByKey(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select block from lectureblock block")
		  .append(" left join fetch block.reasonEffectiveEnd reason")
		  .append(" inner join fetch block.entry entry")
		  .append(" where block.key=:blockKey");

		List<LectureBlock> blocks = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), LectureBlock.class)
				.setParameter("blockKey", key)
				.getResultList();
		return blocks == null || blocks.isEmpty() ? null : blocks.get(0);
	}
	
	public List<LectureBlock> loadByEntry(RepositoryEntryRef entryRef) {
		StringBuilder sb = new StringBuilder();
		sb.append("select block from lectureblock block where block.entry.key=:entryKey");
		
		List<LectureBlock> blocks = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), LectureBlock.class)
				.setParameter("entryKey", entryRef.getKey())
				.getResultList();
		return blocks;
	}
	
	/**
	 * Delete the relation to group, the roll call, the reminders and at the
	 * end the lecture block itself.
	 * 
	 * @param lectureBlock The block to delete
	 * @return The number of rows deleted
	 */
	public int delete(LectureBlock lectureBlock) {
		LectureBlock reloadedBlock = dbInstance.getCurrentEntityManager()
			.getReference(LectureBlockImpl.class, lectureBlock.getKey());
		
		//delete lecture block to group
		String deleteToGroup = "delete from lectureblocktogroup blocktogroup where blocktogroup.lectureBlock.key=:lectureBlockKey";
		int rows = dbInstance.getCurrentEntityManager()
			.createQuery(deleteToGroup)
			.setParameter("lectureBlockKey", reloadedBlock.getKey())
			.executeUpdate();
		//delete LectureBlockRollCallImpl
		String deleteRollCall = "delete from lectureblockrollcall rollcall where rollcall.lectureBlock.key=:lectureBlockKey";
		rows += dbInstance.getCurrentEntityManager()
			.createQuery(deleteRollCall)
			.setParameter("lectureBlockKey", reloadedBlock.getKey())
			.executeUpdate();
		//delete LectureBlockReminderImpl
		String deleteReminder = "delete from lecturereminder reminder where reminder.lectureBlock.key=:lectureBlockKey";
		rows += dbInstance.getCurrentEntityManager()
			.createQuery(deleteReminder)
			.setParameter("lectureBlockKey", reloadedBlock.getKey())
			.executeUpdate();

		dbInstance.getCurrentEntityManager()
			.remove(reloadedBlock);
		rows++;
		
		return rows;
	}
	
	/**
	 * 
	 * @param entry The course (mandatory)
	 * @param teacher The teacher (optional)
	 * @return
	 */
	public List<LectureBlockWithTeachers> getLecturesBlockWithTeachers(RepositoryEntryRef entry) {
		List<LectureBlock> blocks = loadByEntry(entry);
		Map<Long,LectureBlockWithTeachers> blockMap = new HashMap<>();
		for(LectureBlock block:blocks) {
			blockMap.put(block.getKey(), new  LectureBlockWithTeachers(block));
		}
		
		// append the coaches
		StringBuilder sc = new StringBuilder();
		sc.append("select block.key, coach")
		  .append(" from lectureblock block")
		  .append(" inner join block.teacherGroup tGroup")
		  .append(" inner join tGroup.members membership")
		  .append(" inner join membership.identity coach")
		  .append(" inner join fetch coach.user usercoach")
		  .append(" where membership.role='").append("teacher").append("' and block.entry.key=:repoEntryKey");
		
		//get all, it's quick
		List<Object[]> rawCoachs = dbInstance.getCurrentEntityManager()
				.createQuery(sc.toString(), Object[].class)
				.setParameter("repoEntryKey", entry.getKey())
				.getResultList();
		for(Object[] rawCoach:rawCoachs) {
			Long blockKey = (Long)rawCoach[0];
			Identity coach = (Identity)rawCoach[1];
			LectureBlockWithTeachers block = blockMap.get(blockKey);
			if(block != null) {
				block.getTeachers().add(coach);
			}
		}
		return new ArrayList<>(blockMap.values());
	}
	
	/**
	 * 
	 * @param entry The course (mandatory)
	 * @param teacher The teacher (mandatory)
	 * @return
	 */
	public List<LectureBlockWithTeachers> getLecturesBlockWithTeachers(RepositoryEntryRef entry, IdentityRef teacher) {
		List<LectureBlock> blocks = loadByEntry(entry);
		Map<Long,LectureBlockWithTeachers> blockMap = new HashMap<>();
		for(LectureBlock block:blocks) {
			blockMap.put(block.getKey(), new  LectureBlockWithTeachers(block));
		}
		
		// append the coaches
		StringBuilder sc = new StringBuilder();
		sc.append("select block.key, coach")
		  .append(" from lectureblock block")
		  .append(" inner join block.teacherGroup tGroup")
		  .append(" inner join tGroup.members membership")
		  .append(" inner join membership.identity coach")
		  .append(" inner join fetch coach.user usercoach")
		  .append(" where membership.role='").append("teacher").append("' and block.entry.key=:repoEntryKey")
		  .append(" and exists (select teachership.key from bgroupmember teachership where")
		  .append("  teachership.group.key=tGroup.key and teachership.identity.key=:teacherKey")
		  .append(" )");
		
		//get all, it's quick
		List<Object[]> rawCoachs = dbInstance.getCurrentEntityManager()
				.createQuery(sc.toString(), Object[].class)
				.setParameter("repoEntryKey", entry.getKey())
				.setParameter("teacherKey", teacher.getKey())
				.getResultList();
		for(Object[] rawCoach:rawCoachs) {
			Long blockKey = (Long)rawCoach[0];
			Identity coach = (Identity)rawCoach[1];
			LectureBlockWithTeachers block = blockMap.get(blockKey);
			if(block != null) {
				block.getTeachers().add(coach);
			}
		}
		return new ArrayList<>(blockMap.values());
	}

	public LectureBlock addGroupToLectureBlock(LectureBlock block, Group group) {
		LectureBlockImpl reloadedBlock = (LectureBlockImpl)loadByKey(block.getKey());
		LectureBlockToGroupImpl blockToGroup = new LectureBlockToGroupImpl();
		blockToGroup.setGroup(group);
		blockToGroup.setLectureBlock(block);
		dbInstance.getCurrentEntityManager().persist(blockToGroup);
		return reloadedBlock;
	}
	
	public boolean hasLecturesAsTeacher(RepositoryEntryRef entry, IdentityRef identity) {
		StringBuilder sb = new StringBuilder();
		sb.append("select block.key from lectureblock block")
		  .append(" inner join block.teacherGroup teacherGroup")
		  .append(" inner join teacherGroup.members teachers")
		  .append(" where block.entry.key=:entryKey and teachers.identity.key=:identityKey")
		  .append(" and exists (select config.key from lectureentryconfig config")
		  .append("   where config.entry.key=:entryKey and config.lectureEnabled=true")
		  .append(" )");
		
		List<Long> firstKey = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("entryKey", entry.getKey())
				.setParameter("identityKey", identity.getKey())
				.setFirstResult(0)
				.setMaxResults(1)
				.getResultList();
		return firstKey != null && firstKey.size() > 0
				&& firstKey.get(0) != null && firstKey.get(0).longValue() > 0;
	}
	
	public List<LectureBlock> getLecturesAsTeacher(RepositoryEntryRef entry, IdentityRef identity) {
		StringBuilder sb = new StringBuilder();
		sb.append("select block from lectureblock block")
		  .append(" inner join block.teacherGroup teacherGroup")
		  .append(" inner join teacherGroup.members teachers")
		  .append(" where block.entry.key=:entryKey and teachers.identity.key=:identityKey");
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), LectureBlock.class)
				.setParameter("entryKey", entry.getKey())
				.setParameter("identityKey", identity.getKey())
				.getResultList();
	}
	
	public List<Identity> getTeachers(RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct ident from lectureblock block")
		  .append(" inner join block.teacherGroup teacherGroup")
		  .append(" inner join teacherGroup.members membership")
		  .append(" inner join membership.identity ident")
		  .append(" inner join fetch ident.user identUser")
		  .append(" where block.entry.key=:repoKey");
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("repoKey", entry.getKey())
				.getResultList();
	}
	
	public List<Identity> getParticipants(LectureBlockRef block) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct ident from lectureblock block")
		  .append(" inner join block.groups as blockToGroup")
		  .append(" inner join blockToGroup.group as bGroup")
		  .append(" inner join bGroup.members participants on (participants.role='").append(GroupRoles.participant.name()).append("')")
		  .append(" inner join participants.identity ident")
		  .append(" inner join fetch ident.user identUser")
		  .append(" where block.key=:blockKey");
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("blockKey", block.getKey())
				.getResultList();
	}
	
	public List<Identity> getParticipants(RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct ident from lectureblock block")
		  .append(" inner join block.groups as blockToGroup")
		  .append(" inner join blockToGroup.group as bGroup")
		  .append(" inner join bGroup.members participants on (participants.role='").append(GroupRoles.participant.name()).append("')")
		  .append(" inner join participants.identity ident")
		  .append(" inner join fetch ident.user identUser")
		  .append(" where block.entry.key=:repoKey");
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("repoKey", entry.getKey())
				.getResultList();
	}
	
	public List<Identity> getParticipants(RepositoryEntryRef entry, IdentityRef teacher) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct ident from lectureblock block")
		  .append(" inner join block.teacherGroup teacherGroup")
		  .append(" inner join teacherGroup.members teachers on (teachers.identity.key=:teacherKey)")
		  .append(" inner join block.groups as blockToGroup")
		  .append(" inner join blockToGroup.group as bGroup")
		  .append(" inner join bGroup.members participants on (participants.role='").append(GroupRoles.participant.name()).append("')")
		  .append(" inner join participants.identity ident")
		  .append(" inner join fetch ident.user identUser")
		  .append(" where block.entry.key=:repoKey");
		
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("repoKey", entry.getKey())
				.setParameter("teacherKey", teacher.getKey())
				.getResultList();
	}
	
	public List<LectureBlock> loadOpenBlocksBefore(Date endDate) {
		StringBuilder sb = new StringBuilder();
		sb.append("select block from lectureblock block")
		  .append(" left join fetch block.reasonEffectiveEnd reason")
		  .append(" inner join fetch block.entry entry")
		  .append(" where block.endDate<=:endDate and block.rollCallStatusString in ('").append(LectureRollCallStatus.open.name()).append("')");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), LectureBlock.class)
				.setParameter("endDate", endDate, TemporalType.TIMESTAMP)
				.getResultList();
	}
}
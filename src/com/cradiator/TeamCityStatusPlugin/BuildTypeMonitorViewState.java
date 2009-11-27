/*
   Copyright (c) 2007-2009 Nat Pryce.

   This file is part of Team Piazza.

   Team Piazza is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   Team Piazza is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.cradiator.TeamCityStatusPlugin;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsModification;

import java.util.*;


public class BuildTypeMonitorViewState {
	private final SBuildType buildType;

	private final List<String> commitMessages;
	private Build lastFinishedBuild;
    private final Build latestBuild;
    private final Set<String> committers;

    public BuildTypeMonitorViewState(SBuildType buildType) {
		this.buildType = buildType;
        this.lastFinishedBuild = buildType.getLastChangesFinished();
		this.latestBuild = buildType.getLastChangesStartedBuild();
		this.commitMessages = commitMessagesForBuild(latestBuild);

        committers = committersForBuild(latestBuild);
	}

    private Set<String> committersForBuild(Build latestBuild) {
		List<? extends VcsModification> changesSinceLastSuccessfulBuild = changesInBuild(latestBuild);

		HashSet<String> committers = new HashSet<String>();
		for (VcsModification vcsModification : changesSinceLastSuccessfulBuild) {
            String userName = vcsModification.getUserName();
            if (userName != null) {
			    committers.add(userName.trim());
            }
		}
		return committers;
	}

	private ArrayList<String> commitMessagesForBuild(Build latestBuild) {
		List<? extends VcsModification> changesSinceLastSuccessfulBuild = changesInBuild(latestBuild);

		ArrayList<String> commitMessages = new ArrayList<String>();
		for (VcsModification vcsModification : changesSinceLastSuccessfulBuild) {
			commitMessages.add(vcsModification.getDescription().trim());
		}

		return commitMessages;
	}

	@SuppressWarnings("unchecked")
	private List<? extends VcsModification> changesInBuild(Build latestBuild) {
		return latestBuild.getChanges(SelectPrevBuildPolicy.SINCE_LAST_SUCCESSFULLY_FINISHED_BUILD, true);
	}

	public String getFullName() {
		return buildType.getFullName();
	}

    public String getName() {
        return buildType.getName();
    }

	public String getBuildNumber() {
		return latestBuild.getBuildNumber();
	}

	public String getCombinedStatusClasses() {
        return status().toStringReflectingCurrentlyBuilding(isBuilding());
	}

	public boolean isBuilding() {
		return !latestBuild.isFinished();
	}

    public String getActivityMessage() {
		return this.isBuilding() ? "Building" : "Sleeping";
	}

    public boolean isFailing() {
        return this.status() == BuildStatus.FAILURE;
    }

	public Build getLatestBuild() {
		return latestBuild;
	}

    public Build getLastFinishedBuild() {
        return lastFinishedBuild;
    }

	public String getActivity() {
		if (isBuilding()) {
			return ((SRunningBuild)latestBuild).getShortStatistics().getCurrentStage().replace('"','\'');
		}
		else {
			return status().toString().replace('"','\'');
		}
	}

	public int getCompletedPercent() {
		if (isBuilding()) {
			return ((SRunningBuild)latestBuild).getCompletedPercent();
		}
		else {
			return 100;
		}
	}

	public long getDurationSeconds() {
		Date start = latestBuild.getStartDate();
		Date finished = latestBuild.getFinishDate();
		Date end = (finished != null) ? finished : now();

		return (end.getTime() - start.getTime())/1000L;
	}

	private Date now() {
		return new Date();
	}

    public String getStatus() {
        return status().toString();
    }

	public BuildStatus status() {
		if (latestBuild == null) {
			return BuildStatus.UNKNOWN;
		}
		else if (latestBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		}
		if (lastFinishedBuild == null) {
			return BuildStatus.UNKNOWN;
		}
		else if (lastFinishedBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		}
		else {
			return BuildStatus.SUCCESS;
		}
	}

    public String getRunningBuildStatus() {
        return runningBuildStatus().toString();
    }

	public BuildStatus runningBuildStatus() {
		if (latestBuild == null) {
			return BuildStatus.UNKNOWN;
		}
		else if (latestBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		}
		else {
			return BuildStatus.SUCCESS;
		}
	}

	public List<String> getCommitMessages() {
		return commitMessages;
	}

	public Set<String> getCommitters() {
		return committers;
	}

    public String getCommittersString() {
        return committersForBuild(latestBuild).toString();
    }
}

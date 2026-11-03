import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.ScheduleTrigger

object Constants {
    val gitBranches = """
        +:<default>
        +:release/*
        +:hotfix/*
        +:develop
        +:tags/*
    """.trimIndent()
}

project {
    template(TestKitchen_DefaultsTemplate)
    template(TestKitchen_BuildPullRequestsTemplate)
    buildType(TestKitchen_Intune)
    buildType(TestKitchen_AdHoc)
    buildType(TestKitchen_AdHocFreeForm)
    buildType(TestKitchen_Chocolatey)
    buildType(TestKitchen_ChocolateyLicensed)
    buildType(TestKitchen_ChocolateyAgent)
    buildType(TestKitchen_ChocolateyCentralManagement)
    buildType(TestKitchen_Proxy)
    buildType(TestKitchen_SoundnessChecks)
    buildType(TestKitchen_LicensingPortal)
    buildType(TestKitchen_CommunityRepository)
}

object TestKitchen_DefaultsTemplate : Template({
    name = "Test Kitchen Build Template"
    id = AbsoluteId("TestKitchen_DefaultsTemplate")

    artifactRules = """
        +:testresults/**/*.*
        +:logs/**/*.*
        +:.kitchen/logs/*.log
        +:setup-tests.log
    """.trimIndent()
    publishArtifacts = PublishMode.ALWAYS

    params {
        // AWS Settings
        password("env.AWS_ACCESS_KEY_ID", "credentialsJSON:675c8d30-a17f-50c2-8838-847ea9c950b8", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("env.AWS_SECRET_ACCESS_KEY", "credentialsJSON:3fa728d8-a138-5db7-91f6-7d6acb32619c", display = ParameterDisplay.HIDDEN, readOnly = true)
        text("env.AWS_REGION", "us-east-1", allowEmpty = false)
        text("env.AWS_DEFAULT_OUTPUT", "json", allowEmpty = false)
        text("kitchen-pester.version", "1.2.0", label = "kitchen-pester gem version", description = "Version of the kitchen-pester gem to run the test-kitchen execution with", allowEmpty = false)
        // Source Repository settings
        text("env.TK_REPOSITORY_URL", "https://hermes.chocolatey.org:8443/repository/choco-internal-testing/", label = "Repository Url", allowEmpty = false)
        text("env.TK_REPOSITORY_SMOKE_TESTS_URL", "https://hermes.chocolatey.org:8443/repository/choco-test-packages/", label = "Smoke Package Repository Url")
        text("env.TK_REPOSITORY_ALL_PACKAGES_URL", "https://hermes.chocolatey.org:8443/repository/choco-all-test-packages/", label = "All Packages Repository Url")
        text("env.TK_SOURCE_USERNAME", "test-kitchen", label = "Source Repository Username", allowEmpty = false)
        password("env.TK_SOURCE_PASSWORD", "credentialsJSON:ce689a99-23eb-5d58-a3db-0279fa18d50d", label = "Source Repository Password")
        text("env.TK_DOWNLOAD_USERNAME", "test-kitchen", label = "Download Repository Username", allowEmpty = false)
        password("env.TK_DOWNLOAD_PASSWORD", "credentialsJSON:d66f2bcc-7150-5779-ae5b-7c3052e96b6b", label = "Download Repository Password")
        // Git settings to pull tests in
        text("Chocolatey.Branch", "develop", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("Chocolatey.Repo", "chocolatey/choco.git", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyLicensedExtension.Branch", "develop", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyLicensedExtension.Repo", "chocolatey/choco-licensed.git", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyAgent.Branch", "develop", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyAgent.Repo", "chocolatey/choco-licensed-services.git", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyCentralManagement.Branch", "develop", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("ChocolateyCentralManagement.Repo", "chocolatey/choco-licensed-management-ui.git", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("LicensingPortal.Branch", "develop", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("LicensingPortal.Repo", "chocolatey/licensing-administration/licensing-services.git", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("CommunityRepository.Repo", "chocolatey/community-infrastructure/community.chocolatey.org", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("CommunityRepository.Branch", "master", allowEmpty = false, display = ParameterDisplay.PROMPT)
        // Number of Concurrent Kitchens for Verify step. This reduces the duplication of steps
        param("Kitchen.Converge.Concurrency", "10")
        text("env.TK_TAGS", "", label = "Pester Tags", description = "Enter one or more tags, separated with commas, to filter the test suite being run.", validationMessage = "Please enter a comma-separated list of tags.", display = ParameterDisplay.PROMPT)
        text("env.TK_EXCLUDED_TAGS", "", label = "Pester Tags to Exclude", description = "Enter one or more tags, separated with commas, to exclude from the test suite being run.", validationMessage = "Please enter a comma-separated list of tags.", display = ParameterDisplay.PROMPT)
        password("env.TK_API_KEY", "credentialsJSON:4de2cfae-a285-5299-9c1a-13f7c1bf0981", label = "API Key for Repository used by CCR push testing")
        text("env.TK_PUSH_REPO", "https://push.chocolatey.org/", label = "Repository for push CCR tests", description = "Enter the base URL for the repository to use for CCR push tests.")
        password("env.TK_NUGET_API_KEY", "credentialsJSON:d4da481f-27d8-55f6-9dea-4fdba4b23808", label = "API Key for Repository used by NuGet push testing")
        text("env.TK_NUGET_PUSH_REPO", "https://hermes.chocolatey.org:8443/repository/choco-nuget-testing/", label = "Repository for push NuGet tests", description = "Enter the base URL for the repository to use for NuGet push tests.")
        password("env.TK_ADMIN_PASSWORD", "credentialsJSON:9ec98623-7ded-5f57-a20a-13549887fe38", label = "Administrator password for Test Kitchen instances")
        select("TestSuite", "ccm-windows-", label = "Test Suite", description = "Select the Test Kitchen suite to run", display = ParameterDisplay.PROMPT, options = listOf("CCM" to "ccm-windows-", "Chocolatey Agent" to "chocolatey-service-windows-", "Chocolatey Licensed" to "chocolatey-licensed-windows-", "Chocolatey" to "chocolatey-windows-", "Soundness Check" to "soundness-windows-", "Licensing Portal" to "licensing-portal-windows-", "Community Repository" to "community-repository-windows-"))
        select("env.TK_LICENSE_TYPE", "business", label = "License Type", options = listOf("business", "architect", "msp", "professional", "trial"), display = ParameterDisplay.PROMPT)
        // Product versions
        text("env.TK_CHOCOLATEY_VERSION", "pre", label = "Chocolatey Version", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("env.TK_CHOCOLATEY_LICENSED_VERSION", "pre", label = "Chocolatey Licensed Extension Version", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("env.TK_CHOCOLATEY_AGENT_VERSION", "pre", label = "Chocolatey Agent Version", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("env.TK_CCM_VERSION", "pre", label = "Chocolatey Central Management Version", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("env.TK_LICENSING_PORTAL_VERSION", "pre", label = "Licensing Portal Version", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("env.TK_ASPNETCORE_ENVIRONMENT", "Staging", label = "ASP.NET Core Environment", display = ParameterDisplay.PROMPT, allowEmpty = false)
	checkbox("env.TK_USE_VNEXT_REQUIREMENTS", description = "Use vNext Requirements", display = ParameterDisplay.PROMPT, value = "false", checked = "true", unchecked = "false")
    }

    steps {
        script {
            name = "Install / Update Gems"
            scriptContent = "chef gem install kitchen-pester --version %kitchen-pester.version%"
        }

        powerShell {
            name = "Get Chocolatey Tests"
            scriptMode = file {
                path = "Setup-Tests.ps1"
            }
            param("jetbrains_powershell_scriptArguments", "-ChocolateyRepository %Chocolatey.Repo% -ChocolateyBranch %Chocolatey.Branch% -ChocolateyLicensedExtensionRepository %ChocolateyLicensedExtension.Repo% -ChocolateyLicensedExtensionBranch %ChocolateyLicensedExtension.Branch% -ChocolateyAgentRepository %ChocolateyAgent.Repo% -ChocolateyAgentBranch %ChocolateyAgent.Branch% -ChocolateyCentralManagementRepository %ChocolateyCentralManagement.Repo% -ChocolateyCentralManagementBranch %ChocolateyCentralManagement.Branch% -LicensingPortalRepository %LicensingPortal.Repo% -LicensingPortalBranch %LicensingPortal.Branch% -CommunityRepositoryRepository %CommunityRepository.Repo% -CommunityRepositoryBranch %CommunityRepository.Branch% -GitLabPAT %system.GitLabPAT%")
        }

        script {
            name = "Kitchen Create"
            scriptContent = "kitchen create '%TestSuite%' -c"
        }

        powerShell {
            name = "Kitchen Converge"
            scriptMode = script {
                content = """
                    kitchen converge '%TestSuite%' -c
                    ${'$'}ConvergeKnownFailures = select-string -Pattern "NoMethodError: undefined method ``\[\]' for false:FalseClass" -Path .\.kitchen\logs\*.log

                    if (${'$'}LastExitCode -ne 0 -and (${'$'}null -ne ${'$'}ConvergeKnownFailures)) {
                        Write-Warning "Known failure found in files: '${'$'}(${'$'}ConvergeKnownFailures.FileName -join ', ')' running again."
                        kitchen converge '%TestSuite%' -c
                    }

                    exit ${'$'}LastExitCode
                """.trimIndent()
            }
        }

        script {
            name = "Kitchen Verify"
            scriptContent = "kitchen verify '%TestSuite%' -c %Kitchen.Converge.Concurrency%"
        }

        script {
            name = "Kitchen Destroy"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = "kitchen destroy '%TestSuite%' -c --no-log-overwrite"
        }

        script {
            name = "Delete AWS Volumes"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = "chef exec ruby ./delete_ec2_volumes.rb"
        }
    }

    vcs {
        root(DslContext.settingsRoot)
        cleanCheckout = true
    }

    failureConditions {
        executionTimeoutMin = 1080
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.TEST_FAILED_COUNT
            threshold = 0
            units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
            comparison = BuildFailureOnMetric.MetricComparison.MORE
            compareTo = value()
            param("anchorBuild", "lastSuccessful")
        }
    }

    features {
        xmlReport {
            reportType = XmlReport.XmlReportType.NUNIT
            rules = "testresults/**/TestResults.xml"
        }
        xmlReport {
            reportType = XmlReport.XmlReportType.JUNIT
            rules = "testresults/**/playwright-results/**/*.xml"
        }
        pullRequests {
            provider = gitlab {
                authType = token {
                    token = "%system.GitLabPAT%"
                }
            }
        }
    }
})

object TestKitchen_BuildPullRequestsTemplate : Template({
    name = "Test Kitchen Build Pull Requests Template"
    id = AbsoluteId("TestKitchen_BuildPullRequestsTemplate")

    triggers {
        vcs {
            branchFilter = ""
        }
    }
})

object TestKitchen_Intune : BuildType({
    id = AbsoluteId("TestKitchen_Intune")
    name = "Intune"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    maxRunningBuilds = 1

    params {
        param("TestSuite", "chocolatey-licensed-windows-")
        param("Kitchen.Converge.Concurrency", "1")
        param("env.TK_TAGS", "Intune")
        password("env.TK_INTUNE_CLIENT_SECRET", "credentialsJSON:5d5bf89a-4b27-5141-8482-0254b895d518", label = "Intune Client Secret", description = "Enter the Intune Client Secret", display = ParameterDisplay.PROMPT)
        password("env.TK_INTUNE_TENANT", "credentialsJSON:42f21000-d544-5860-8bcc-120855b35ee4", label = "Intune Tenant", description = "Enter the Intune Tenant", display = ParameterDisplay.PROMPT)
        password("env.TK_INTUNE_CLIENT_ID", "credentialsJSON:28725680-619b-565c-bb2e-ebf6ba828833", label = "Intune Client ID", description = "Enter the Intune Client ID", display = ParameterDisplay.PROMPT)
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Friday
                hour = 20
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }
})

object TestKitchen_AdHoc : BuildType({
    id = AbsoluteId("TestKitchen_AdHoc")
    name = "Ad Hoc"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))
})

object TestKitchen_AdHocFreeForm : BuildType({
    id = AbsoluteId("TestKitchen_AdHocFreeForm")
    name = "Ad Hoc (Free-form Suite)"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "", label = "Test Suite", description = "Enter the Test Kitchen suite name, or a regular expression matching several suites, to run", allowEmpty = false, display = ParameterDisplay.PROMPT)
    }
})

object TestKitchen_Chocolatey : BuildType({
    id = AbsoluteId("TestKitchen_Chocolatey")
    name = "1 Chocolatey"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "chocolatey-windows-")
        text("env.TK_CHOCOLATEY_VERSION", "%dep.Chocolatey.build.number%")
        text("Chocolatey.Branch", "%dep.Chocolatey.teamcity.build.branch%")
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Friday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("Chocolatey")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }
            artifacts {
                artifactRules = "chocolatey.%env.TK_CHOCOLATEY_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
    }
})

object TestKitchen_ChocolateyLicensed : BuildType({
    id = AbsoluteId("TestKitchen_ChocolateyLicensed")
    name = "2 Chocolatey Licensed"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "chocolatey-licensed-windows-")
        text("env.TK_CHOCOLATEY_VERSION", "%dep.Chocolatey.build.number%")
        text("Chocolatey.Branch", "%dep.Chocolatey.teamcity.build.branch%")
        text("env.TK_CHOCOLATEY_LICENSED_VERSION", "%dep.ChocolateyLicensed.build.number%")
        text("ChocolateyLicensedExtension.Branch", "%dep.ChocolateyLicensed.teamcity.build.branch%")
        // Intune variables defaulting to blank. This allows us to feed them into a PR build for CLE without leaking them.
        password("env.TK_INTUNE_CLIENT_SECRET", "", label = "Intune Client Secret", description = "Enter the Intune Client Secret", display = ParameterDisplay.PROMPT)
        password("env.TK_INTUNE_TENANT", "", label = "Intune Tenant", description = "Enter the Intune Tenant", display = ParameterDisplay.PROMPT)
        password("env.TK_INTUNE_CLIENT_ID", "", label = "Intune Client ID", description = "Enter the Intune Client ID", display = ParameterDisplay.PROMPT)
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Saturday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("Chocolatey")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }

            artifacts {
                artifactRules = "chocolatey.%env.TK_CHOCOLATEY_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyLicensed")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = "chocolatey.extension.%env.TK_CHOCOLATEY_LICENSED_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
    }
})

object TestKitchen_ChocolateyAgent : BuildType({
    id = AbsoluteId("TestKitchen_ChocolateyAgent")
    name = "3 Chocolatey Agent"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "chocolatey-service-windows-")
        text("env.TK_CHOCOLATEY_VERSION", "%dep.Chocolatey.build.number%")
        text("Chocolatey.Branch", "%dep.Chocolatey.teamcity.build.branch%")
        text("env.TK_CHOCOLATEY_LICENSED_VERSION", "%dep.ChocolateyLicensed.build.number%")
        text("ChocolateyLicensedExtension.Branch", "%dep.ChocolateyLicensed.teamcity.build.branch%")
        text("env.TK_CHOCOLATEY_AGENT_VERSION", "%dep.ChocolateyAgent.build.number%")
        text("ChocolateyAgent.Branch", "%dep.ChocolateyAgent.teamcity.build.branch%")
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Sunday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("Chocolatey")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }

            artifacts {
                artifactRules = "chocolatey.%env.TK_CHOCOLATEY_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyLicensed")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = "chocolatey.extension.%env.TK_CHOCOLATEY_LICENSED_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyAgent")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = "chocolatey-agent.%env.TK_CHOCOLATEY_AGENT_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
    }
})

object TestKitchen_ChocolateyCentralManagement : BuildType({
    id = AbsoluteId("TestKitchen_ChocolateyCentralManagement")
    name = "4 Chocolatey Central Management"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "ccm-windows-")
        text("env.TK_CHOCOLATEY_VERSION", "%dep.Chocolatey.build.number%")
        text("Chocolatey.Branch", "%dep.Chocolatey.teamcity.build.branch%")
        text("env.TK_CHOCOLATEY_LICENSED_VERSION", "%dep.ChocolateyLicensed.build.number%")
        text("ChocolateyLicensedExtension.Branch", "%dep.ChocolateyLicensed.teamcity.build.branch%")
        text("env.TK_CHOCOLATEY_AGENT_VERSION", "%dep.ChocolateyAgent.build.number%")
        text("ChocolateyAgent.Branch", "%dep.ChocolateyAgent.teamcity.build.branch%")
        text("env.TK_CCM_VERSION", "%dep.ChocolateyCentralManagement.build.number%")
        text("ChocolateyCentralManagement.Branch", "%dep.ChocolateyCentralManagement.teamcity.build.branch%")
        text("env.TK_TAGS", "CCM")
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Monday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("Chocolatey")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }

            artifacts {
                artifactRules = "chocolatey.%env.TK_CHOCOLATEY_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyLicensed")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = "chocolatey.extension.%env.TK_CHOCOLATEY_LICENSED_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyAgent")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = "chocolatey-agent.%env.TK_CHOCOLATEY_AGENT_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
        dependency(AbsoluteId("ChocolateyCentralManagement")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
            }

            artifacts {
                artifactRules = """
                    chocolatey-management-database.%env.TK_CCM_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages
                    chocolatey-management-service.%env.TK_CCM_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages
                    chocolatey-management-web.%env.TK_CCM_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages
                """.trimIndent()
            }
        }
    }
})

object TestKitchen_Proxy : BuildType({
    id = AbsoluteId("TestKitchen_Proxy")
    name = "Proxy"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        select("TestSuite", "chocolatey-windows-", label = "Test Suite", description = "Select the Test Kitchen suite to run", display = ParameterDisplay.PROMPT, options = listOf("Chocolatey Agent" to "chocolatey-service-windows-", "Chocolatey Licensed" to "chocolatey-licensed-windows-", "Chocolatey" to "chocolatey-windows-"))
        select("env.TK_PROXY_CONFIGURATION", "", label = "Proxy Configuration", description = "Select the proxy configuration to use for these tests", display = ParameterDisplay.PROMPT, options = listOf("Chocolatey Configuration" to "Configuration", "Chocolatey Configuration (Authenticated Proxy)" to "ConfigurationAuthenticated", "Command Line Arguments" to "CLI", "Command Line Arguments (Authenticated Proxy)" to "CLIAuthenticated", "System level" to "System", "http(s)_proxy Environment Variables" to "SystemEnvironmentVariables"))
        text("env.TK_PROXY_SERVER", "ip-172-30-10-187.ec2.internal")
        password("env.TK_PROXY_USERNAME", "credentialsJSON:44d3ab67-6c90-5fe0-b7d5-a812e062390a", display = ParameterDisplay.PROMPT)
        password("env.TK_PROXY_PASSWORD", "credentialsJSON:a3fd8851-22ce-50bc-aa68-ff0abf4459d3", display = ParameterDisplay.PROMPT)
        text("env.TK_EXCLUDED_TAGS", "ProxySkip")
    }
})

object TestKitchen_SoundnessChecks : BuildType({
    id = AbsoluteId("TestKitchen_SoundnessChecks")
    name = "Soundness Checks"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "soundness-windows-")
        text("env.TK_CHOCOLATEY_GUI_VERSION", "pre", label = "Chocolatey GUI Version", allowEmpty = false, display = ParameterDisplay.PROMPT)
        text("env.TK_CHOCOLATEY_GUI_LICENSED_VERSION", "pre", label = "Chocolatey GUI Extension Version", allowEmpty = false, display = ParameterDisplay.PROMPT)
    }
})

object TestKitchen_LicensingPortal : BuildType({
    id = AbsoluteId("TestKitchen_LicensingPortal")
    name = "Licensing Portal"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "licensing-portal-windows-")
        text("env.TK_LICENSING_PORTAL_VERSION", "%dep.LicensingPortal.build.number%")
        text("LicensingPortal.Branch", "%dep.LicensingPortal.teamcity.build.branch%")
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Tuesday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("LicensingPortal")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }
            artifacts {
                artifactRules = "licensingservices-web.%env.TK_LICENSING_PORTAL_VERSION%.nupkg=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
    }
})

object TestKitchen_CommunityRepository : BuildType({
    id = AbsoluteId("TestKitchen_CommunityRepository")
    name = "Community Repository"
    templates(AbsoluteId("TestKitchen_DefaultsTemplate"),AbsoluteId("SlackNotificationTemplate"))

    params {
        text("TestSuite", "community-repository-windows-")
        text("CommunityRepository.Branch", "%dep.CommunityRepository_ChocolateyCommunityRepository.teamcity.build.branch%")
    }

    triggers {
        schedule {
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Wednesday
                hour = 3
                minute = 0
            }
            branchFilter = """
                +:<default>
            """.trimIndent()
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    dependencies {
        dependency(AbsoluteId("CommunityRepository_ChocolateyCommunityRepository")) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
                synchronizeRevisions = false
                reuseBuilds = ReuseBuilds.SUCCESSFUL
            }
            artifacts {
                artifactRules = "*.*=>%system.teamcity.build.checkoutDir%/packages"
            }
        }
    }
})

USE [event_management_db];
GO

SET NOCOUNT ON;

IF COL_LENGTH('dbo.event', 'image_url') IS NULL
BEGIN
    ALTER TABLE dbo.event ADD image_url NVARCHAR(500) NULL;
END;
GO

IF COL_LENGTH('dbo.event', 'image_urls') IS NULL
BEGIN
    ALTER TABLE dbo.event ADD image_urls NVARCHAR(MAX) NULL;
END;
GO

IF COL_LENGTH('dbo.event', 'budget') IS NULL
BEGIN
    ALTER TABLE dbo.event ADD budget DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_event_budget DEFAULT 0;
END;
GO

IF OBJECT_ID('dbo.event_proposal', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.event_proposal', 'location') IS NULL
        ALTER TABLE dbo.event_proposal ADD location NVARCHAR(200) NULL;
    IF COL_LENGTH('dbo.event_proposal', 'capacity') IS NULL
        ALTER TABLE dbo.event_proposal ADD capacity INT NULL;
    IF COL_LENGTH('dbo.event_proposal', 'image_url') IS NULL
        ALTER TABLE dbo.event_proposal ADD image_url NVARCHAR(500) NULL;
    IF COL_LENGTH('dbo.event_proposal', 'image_urls') IS NULL
        ALTER TABLE dbo.event_proposal ADD image_urls NVARCHAR(MAX) NULL;
    IF COL_LENGTH('dbo.event_proposal', 'budget') IS NULL
        ALTER TABLE dbo.event_proposal ADD budget DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_event_proposal_budget DEFAULT 0;
END;
GO

IF OBJECT_ID('dbo.eventProposal', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.eventProposal
    SET capacity = 100
    WHERE capacity IS NULL;

    UPDATE dbo.eventProposal
    SET budget = 0
    WHERE budget IS NULL;

    UPDATE dbo.eventProposal
    SET image_url = N'https://images.unsplash.com/photo-1540575467063-027a26d3b38c?auto=format&fit=crop&w=1200&q=80'
    WHERE image_url IS NULL OR LTRIM(RTRIM(image_url)) = N'';

    UPDATE dbo.eventProposal
    SET image_urls = image_url
    WHERE (image_urls IS NULL OR LTRIM(RTRIM(image_urls)) = N'')
      AND image_url IS NOT NULL
      AND LTRIM(RTRIM(image_url)) <> N'';
END;
GO

IF OBJECT_ID('dbo.eventProposal', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.eventProposal', 'location') IS NULL
        ALTER TABLE dbo.eventProposal ADD location NVARCHAR(200) NULL;
    IF COL_LENGTH('dbo.eventProposal', 'capacity') IS NULL
        ALTER TABLE dbo.eventProposal ADD capacity INT NULL;
    IF COL_LENGTH('dbo.eventProposal', 'image_url') IS NULL
        ALTER TABLE dbo.eventProposal ADD image_url NVARCHAR(500) NULL;
    IF COL_LENGTH('dbo.eventProposal', 'image_urls') IS NULL
        ALTER TABLE dbo.eventProposal ADD image_urls NVARCHAR(MAX) NULL;
    IF COL_LENGTH('dbo.eventProposal', 'budget') IS NULL
        ALTER TABLE dbo.eventProposal ADD budget DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_eventProposal_budget DEFAULT 0;
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.role WHERE name = 'MANAGER')
BEGIN
    INSERT INTO dbo.role (name, description)
    VALUES ('MANAGER', N'Quản lý khoa/bộ môn: phụ trách proposal, event và sinh viên trong đơn vị.');
END;
GO

UPDATE dbo.event
SET budget = 0
WHERE budget IS NULL;
GO

UPDATE dbo.event
SET image_url = CASE
    WHEN title LIKE N'%Hackathon%' OR title LIKE N'%Contest%' OR title LIKE N'%CTF%' THEN
        N'https://images.unsplash.com/photo-1591453089816-0fbb971b454c?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Security%' OR title LIKE N'%An toàn%' OR title LIKE N'%Pentest%' THEN
        N'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%AI%' OR title LIKE N'%GenAI%' OR title LIKE N'%LLM%' OR title LIKE N'%Trí tuệ%' THEN
        N'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Data%' OR title LIKE N'%Analytics%' OR title LIKE N'%Power BI%' OR title LIKE N'%Tableau%' THEN
        N'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Cloud%' OR title LIKE N'%DevOps%' OR title LIKE N'%Kubernetes%' OR title LIKE N'%Docker%' THEN
        N'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%UX%' OR title LIKE N'%Design%' OR title LIKE N'%Thiết kế%' OR title LIKE N'%Figma%' THEN
        N'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Marketing%' OR title LIKE N'%Brand%' OR title LIKE N'%Ads%' THEN
        N'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Startup%' OR title LIKE N'%Pitching%' OR title LIKE N'%Business Case%' THEN
        N'https://images.unsplash.com/photo-1543269664-7eef42226a21?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Finance%' OR title LIKE N'%FinTech%' OR title LIKE N'%Tài chính%' OR title LIKE N'%Ngân hàng%' THEN
        N'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%English%' OR title LIKE N'%IELTS%' OR title LIKE N'%TOEIC%' OR title LIKE N'%Speaking%' THEN
        N'https://images.unsplash.com/photo-1503428593586-e225b39bddfe?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Japan%' OR title LIKE N'%JLPT%' OR title LIKE N'%Nhật%' THEN
        N'https://images.unsplash.com/photo-1542051841857-5f90071e7989?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Hotel%' OR title LIKE N'%Khách sạn%' OR title LIKE N'%Tourism%' OR title LIKE N'%F&B%' THEN
        N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Career%' OR title LIKE N'%Việc làm%' THEN
        N'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Tốt nghiệp%' OR title LIKE N'%Graduation%' THEN
        N'https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80'
    WHEN title LIKE N'%Podcast%' OR title LIKE N'%Video%' OR title LIKE N'%Media%' THEN
        N'https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=1200&q=80'
    ELSE
        N'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80'
END
WHERE image_url IS NULL OR LTRIM(RTRIM(image_url)) = N'';
GO

UPDATE dbo.event
SET image_urls = image_url
WHERE (image_urls IS NULL OR LTRIM(RTRIM(image_urls)) = N'')
  AND image_url IS NOT NULL
  AND LTRIM(RTRIM(image_url)) <> N'';
GO

IF OBJECT_ID('dbo.event_proposal', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.event_proposal
    SET capacity = 100
    WHERE capacity IS NULL;

    UPDATE dbo.event_proposal
    SET budget = 0
    WHERE budget IS NULL;

    UPDATE dbo.event_proposal
    SET image_url = N'https://images.unsplash.com/photo-1540575467063-027a26d3b38c?auto=format&fit=crop&w=1200&q=80'
    WHERE image_url IS NULL OR LTRIM(RTRIM(image_url)) = N'';

    UPDATE dbo.event_proposal
    SET image_urls = image_url
    WHERE (image_urls IS NULL OR LTRIM(RTRIM(image_urls)) = N'')
      AND image_url IS NOT NULL
      AND LTRIM(RTRIM(image_url)) <> N'';
END;
GO

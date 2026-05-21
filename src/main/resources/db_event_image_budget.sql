USE [event_management_db];
GO

SET NOCOUNT ON;

IF COL_LENGTH('dbo.event', 'image_url') IS NULL
BEGIN
    ALTER TABLE dbo.event ADD image_url NVARCHAR(500) NULL;
END;
GO

IF COL_LENGTH('dbo.event', 'budget') IS NULL
BEGIN
    ALTER TABLE dbo.event ADD budget DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_event_budget DEFAULT 0;
END;
GO

UPDATE dbo.event
SET budget = 0
WHERE budget IS NULL;
GO

UPDATE dbo.event
SET image_url = CASE
    WHEN title LIKE N'%Marketing%' OR title LIKE N'%Business%' OR title LIKE N'%kinh tế%' THEN
        N'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80'
    WHEN title LIKE N'%Security%' OR title LIKE N'%CTF%' OR title LIKE N'%An toàn%' THEN
        N'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80'
    WHEN title LIKE N'%AI%' OR title LIKE N'%Data%' OR title LIKE N'%Trí tuệ%' THEN
        N'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80'
    WHEN title LIKE N'%UX%' OR title LIKE N'%Design%' OR title LIKE N'%Thiết kế%' THEN
        N'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80'
    ELSE
        N'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80'
END
WHERE image_url IS NULL OR LTRIM(RTRIM(image_url)) = N'';
GO

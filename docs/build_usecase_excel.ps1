$ErrorActionPreference = 'Stop'

$base = 'd:\fpt\CampusEvent\docs'
$dataPath = Join-Path $base 'usecases_data.json'
$outPath  = Join-Path $base 'UseCase_PhanCong_4ThanhVien.xlsx'

$data = Get-Content -Raw -Encoding UTF8 -Path $dataPath | ConvertFrom-Json
$meta = $data.meta
$members = $data.members
$ucs = $data.usecases
$schedule = $data.schedule

# Map short -> module / name
$moduleOf = @{}
$nameOf = @{}
foreach ($m in $members) { $moduleOf[$m.short] = $m.module; $nameOf[$m.short] = $m.name }

# Colors (Excel uses BGR)
$C_TITLE   = 8210719     # dark blue
$C_HEADER  = 15123099    # light blue
$C_WHITE   = 16777215
$ownerColor = @{
    'Anh'  = 14083324   # light orange
    'Đạt'  = 13434828   # light green
    'Tú'   = 14077694   # light purple
    'Sang' = 14211288   # light grey-blue
}

$xlCenter = -4108
$xlTop    = -4160
$xlLeft   = -4131

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

function Set-Title($ws, $range, $text) {
    $ws.Range($range).Merge() | Out-Null
    $r = $ws.Range($range)
    $r.Value2 = $text
    $r.Font.Bold = $true
    $r.Font.Size = 14
    $r.Interior.Color = $C_TITLE
    $r.Font.Color = $C_WHITE
    $r.HorizontalAlignment = $xlCenter
    $r.RowHeight = 26
}

try {
    $wb = $excel.Workbooks.Add()

    # ============ Sheet 1: TỔNG QUAN ============
    $ws0 = $wb.Worksheets.Item(1)
    $ws0.Name = 'Tổng quan'

    Set-Title $ws0 'A1:D1' $meta.title

    $ws0.Cells.Item(3,1).Value2 = 'Dự án'
    $ws0.Cells.Item(3,2).Value2 = $meta.project
    $ws0.Cells.Item(4,1).Value2 = 'Nhóm'
    $ws0.Cells.Item(4,2).Value2 = $meta.group
    $ws0.Cells.Item(5,1).Value2 = 'Tiến độ'
    $ws0.Cells.Item(5,2).Value2 = '10 tuần (T1: khởi tạo; T2-3: nền tảng/xác thực; T4-6: CRUD & nghiệp vụ chính; T7-8: hoàn thiện; T9: tích hợp & E2E; T10: hồi quy & bàn giao)'
    $ws0.Cells.Item(6,1).Value2 = 'Tổng số Use Case'
    $ws0.Cells.Item(6,2).Value2 = [double]$ucs.Count
    $ws0.Cells.Item(7,1).Value2 = 'Ghi chú'
    $ws0.Cells.Item(7,2).Value2 = $meta.note
    $ws0.Range('A3:A7').Font.Bold = $true

    # Bảng phân công thành viên
    $row = 9
    $ws0.Cells.Item($row,1).Value2 = 'Thành viên'
    $ws0.Cells.Item($row,2).Value2 = 'Mã'
    $ws0.Cells.Item($row,3).Value2 = 'Module phụ trách'
    $ws0.Cells.Item($row,4).Value2 = 'Số UC / Danh sách mã'
    $ws0.Range("A$row`:D$row").Font.Bold = $true
    $ws0.Range("A$row`:D$row").Interior.Color = $C_HEADER
    $ws0.Range("A$row`:D$row").HorizontalAlignment = $xlCenter
    $row++

    foreach ($m in $members) {
        $codes = @()
        foreach ($u in $ucs) { if ($u.owner -eq $m.short) { $codes += $u.code } }
        $ws0.Cells.Item($row,1).Value2 = $m.name
        $ws0.Cells.Item($row,2).Value2 = $m.code
        $ws0.Cells.Item($row,3).Value2 = $m.module
        $ws0.Cells.Item($row,4).Value2 = ("{0} UC: {1}" -f $codes.Count, ($codes -join ', '))
        if ($ownerColor.ContainsKey($m.short)) { $ws0.Range("A$row`:D$row").Interior.Color = $ownerColor[$m.short] }
        $row++
    }
    $totRow = $row
    $ws0.Cells.Item($totRow,1).Value2 = 'TỔNG'
    $ws0.Cells.Item($totRow,4).Value2 = ("{0} Use Case" -f $ucs.Count)
    $ws0.Range("A$totRow`:D$totRow").Font.Bold = $true
    $row += 2

    # Use case đã loại bỏ
    if ($data.removed) {
        $ws0.Cells.Item($row,1).Value2 = 'Use case đã loại bỏ khi rà soát theo Test Case'
        $ws0.Range("A$row`:D$row").Merge() | Out-Null
        $ws0.Range("A$row").Font.Bold = $true
        $ws0.Range("A$row").Interior.Color = $C_HEADER
        $row++
        $ws0.Cells.Item($row,1).Value2 = 'Mục'
        $ws0.Cells.Item($row,2).Value2 = 'Lý do'
        $ws0.Range("B$row`:D$row").Merge() | Out-Null
        $ws0.Range("A$row`:D$row").Font.Bold = $true
        $row++
        foreach ($rm in $data.removed) {
            $ws0.Cells.Item($row,1).Value2 = $rm.code
            $ws0.Cells.Item($row,2).Value2 = $rm.reason
            $ws0.Range("B$row`:D$row").Merge() | Out-Null
            $row++
        }
    }

    $ws0.Columns.Item(1).ColumnWidth = 26
    $ws0.Columns.Item(2).ColumnWidth = 12
    $ws0.Columns.Item(3).ColumnWidth = 52
    $ws0.Columns.Item(4).ColumnWidth = 60
    $area0 = $ws0.Range("A3:D$($row-1)")
    $area0.WrapText = $true
    $area0.VerticalAlignment = $xlTop

    # ============ Sheet 2: PHÂN CÔNG USE CASE ============
    $ws = $wb.Worksheets.Add([System.Reflection.Missing]::Value, $ws0)
    $ws.Name = 'Phân công Use Case'

    Set-Title $ws 'A1:I1' 'PHÂN CÔNG USE CASE THEO THÀNH VIÊN'

    $headers = @('STT','Mã UC','Tên Use Case','Tác nhân','Mô tả','Thành viên','Module','Tuần','Trạng thái')
    for ($c = 0; $c -lt $headers.Count; $c++) {
        $cell = $ws.Cells.Item(2, $c + 1)
        $cell.Value2 = $headers[$c]
        $cell.Font.Bold = $true
        $cell.Interior.Color = $C_HEADER
        $cell.HorizontalAlignment = $xlCenter
    }

    $r = 3
    $i = 1
    foreach ($u in $ucs) {
        $ws.Cells.Item($r,1).Value2 = [double]$i
        $ws.Cells.Item($r,2).Value2 = $u.code
        $ws.Cells.Item($r,3).Value2 = $u.name
        $ws.Cells.Item($r,4).Value2 = $u.actor
        $ws.Cells.Item($r,5).Value2 = $u.desc
        $ws.Cells.Item($r,6).Value2 = $nameOf[$u.owner]
        $ws.Cells.Item($r,7).Value2 = $moduleOf[$u.owner]
        $ws.Cells.Item($r,8).Value2 = ("Tuần {0}" -f $u.week)
        $ws.Cells.Item($r,9).Value2 = 'Hoàn thành 100%'
        if ($ownerColor.ContainsKey($u.owner)) { $ws.Range("A$r`:I$r").Interior.Color = $ownerColor[$u.owner] }
        $r++
        $i++
    }
    $lastRow = $r - 1

    $ws.Columns.Item(1).ColumnWidth = 5
    $ws.Columns.Item(2).ColumnWidth = 8
    $ws.Columns.Item(3).ColumnWidth = 34
    $ws.Columns.Item(4).ColumnWidth = 14
    $ws.Columns.Item(5).ColumnWidth = 60
    $ws.Columns.Item(6).ColumnWidth = 22
    $ws.Columns.Item(7).ColumnWidth = 40
    $ws.Columns.Item(8).ColumnWidth = 9
    $ws.Columns.Item(9).ColumnWidth = 16

    $table = $ws.Range("A2:I$lastRow")
    $table.WrapText = $true
    $table.VerticalAlignment = $xlTop
    $table.Borders.LineStyle = 1
    $table.Borders.Weight = 2

    $ws.Application.ActiveWindow.SplitRow = 2
    $ws.Application.ActiveWindow.FreezePanes = $true

    # ============ Sheet 3: TIẾN ĐỘ 10 TUẦN ============
    $ws3 = $wb.Worksheets.Add([System.Reflection.Missing]::Value, $ws)
    $ws3.Name = 'Tiến độ 10 tuần'

    Set-Title $ws3 'A1:I1' 'TIẾN ĐỘ & MỨC HOÀN THÀNH THEO TUẦN (TUẦN 1 → TUẦN 10)'

    $h3 = @('Tuần','Giai đoạn','Trọng tâm công việc','Use case trong tuần',
            ("Anh (%)"), ("Đạt (%)"), ("Tú (%)"), ("Sang (%)"), 'TB chung (%)')
    for ($c = 0; $c -lt $h3.Count; $c++) {
        $cell = $ws3.Cells.Item(2, $c + 1)
        $cell.Value2 = $h3[$c]
        $cell.Font.Bold = $true
        $cell.Interior.Color = $C_HEADER
        $cell.HorizontalAlignment = $xlCenter
    }

    $rr = 3
    foreach ($s in $schedule) {
        $sumAvg = [double]([string]$s.p.'Anh') + [double]([string]$s.p.'Đạt') + [double]([string]$s.p.'Tú') + [double]([string]$s.p.'Sang')
        $ws3.Cells.Item($rr,1).Value2 = [double]([string]$s.week)
        $ws3.Cells.Item($rr,2).Value2 = [string]$s.phase
        $ws3.Cells.Item($rr,3).Value2 = [string]$s.focus
        $ws3.Cells.Item($rr,4).Value2 = [string]$s.uc
        $ws3.Cells.Item($rr,5).Value2 = [double]([string]$s.p.'Anh')
        $ws3.Cells.Item($rr,6).Value2 = [double]([string]$s.p.'Đạt')
        $ws3.Cells.Item($rr,7).Value2 = [double]([string]$s.p.'Tú')
        $ws3.Cells.Item($rr,8).Value2 = [double]([string]$s.p.'Sang')
        $ws3.Cells.Item($rr,9).Value2 = [double]([math]::Round(($sumAvg / 4.0), 0))
        $rr++
    }
    $lastRow3 = $rr - 1

    $ws3.Columns.Item(1).ColumnWidth = 7
    $ws3.Columns.Item(2).ColumnWidth = 18
    $ws3.Columns.Item(3).ColumnWidth = 64
    $ws3.Columns.Item(4).ColumnWidth = 40
    $ws3.Columns.Item(5).ColumnWidth = 10
    $ws3.Columns.Item(6).ColumnWidth = 10
    $ws3.Columns.Item(7).ColumnWidth = 10
    $ws3.Columns.Item(8).ColumnWidth = 10
    $ws3.Columns.Item(9).ColumnWidth = 13

    $table3 = $ws3.Range("A2:I$lastRow3")
    $table3.WrapText = $true
    $table3.VerticalAlignment = $xlTop
    $table3.Borders.LineStyle = 1
    $table3.Borders.Weight = 2
    $ws3.Range("E3:I$lastRow3").HorizontalAlignment = $xlCenter

    # Conditional color scale on % columns (E:I)
    $pctRange = $ws3.Range("E3:I$lastRow3")
    $cs = $pctRange.FormatConditions.AddColorScale(3)
    $cs.ColorScaleCriteria.Item(1).Type = 1   # xlConditionValueLowestValue
    $cs.ColorScaleCriteria.Item(1).FormatColor.Color = 7039480    # red-ish
    $cs.ColorScaleCriteria.Item(2).Type = 4   # xlConditionValuePercent
    $cs.ColorScaleCriteria.Item(2).Value = 50
    $cs.ColorScaleCriteria.Item(2).FormatColor.Color = 8711167    # yellow
    $cs.ColorScaleCriteria.Item(3).Type = 2   # xlConditionValueHighestValue
    $cs.ColorScaleCriteria.Item(3).FormatColor.Color = 8508442    # green

    $ws3.Application.ActiveWindow.SplitRow = 2
    $ws3.Application.ActiveWindow.FreezePanes = $true

    # Order: Tổng quan, Phân công, Tiến độ
    $ws0.Move($wb.Worksheets.Item(1)) | Out-Null

    if (Test-Path $outPath) {
        try { Remove-Item $outPath -Force } catch { }
    }
    if (Test-Path $outPath) {
        $outPath = Join-Path $base ('UseCase_PhanCong_4ThanhVien_' + (Get-Date -Format 'yyyyMMdd_HHmm') + '.xlsx')
        Write-Output "GHI CHU: File cu dang mo trong Excel, luu sang ten moi."
    }
    $wb.SaveAs($outPath, 51)
    $wb.Close($false)
    Write-Output "SAVED: $outPath"
    Write-Output ("So use case: {0}" -f $ucs.Count)
}
finally {
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel) | Out-Null
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}

param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDirectory,
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'

function Get-SafeValue {
    param([scriptblock]$Expression, $Default = $null)
    try { return & $Expression } catch { return $Default }
}

function Release-ComObject {
    param($Value)
    if ($null -ne $Value -and [Runtime.InteropServices.Marshal]::IsComObject($Value)) {
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($Value)
    }
}

function Convert-PointsToCentimeters {
    param($Points)
    if ($null -eq $Points -or $Points -eq 9999999 -or $Points -eq -9999999) { return $null }
    return [Math]::Round(([double]$Points) * 2.54 / 72.0, 3)
}

function Convert-Points {
    param($Points)
    if ($null -eq $Points -or $Points -eq 9999999 -or $Points -eq -9999999) { return $null }
    return [Math]::Round([double]$Points, 2)
}

function Convert-TriState {
    param($Value)
    if ($Value -eq -1) { return $true }
    if ($Value -eq 0) { return $false }
    return $null
}

function Get-CleanText {
    param([string]$Text)
    if ($null -eq $Text) { return '' }
    return ($Text -replace "[\r\a]+$", '' -replace "\v", "`n")
}

function Get-FontInfo {
    param($Font)
    return [ordered]@{
        name_far_east = Get-SafeValue { [string]$Font.NameFarEast }
        name_ascii = Get-SafeValue { [string]$Font.NameAscii }
        name_other = Get-SafeValue { [string]$Font.NameOther }
        size_pt = Convert-Points (Get-SafeValue { $Font.Size })
        bold = Convert-TriState (Get-SafeValue { $Font.Bold })
        italic = Convert-TriState (Get-SafeValue { $Font.Italic })
        underline = Get-SafeValue { [int]$Font.Underline }
        color = Get-SafeValue { [int]$Font.Color }
        hidden = Convert-TriState (Get-SafeValue { $Font.Hidden })
        superscript = Convert-TriState (Get-SafeValue { $Font.Superscript })
        subscript = Convert-TriState (Get-SafeValue { $Font.Subscript })
        spacing_pt = Convert-Points (Get-SafeValue { $Font.Spacing })
        scaling_percent = Get-SafeValue { [int]$Font.Scaling }
    }
}

function Get-ParagraphFormatInfo {
    param($ParagraphFormat)
    return [ordered]@{
        alignment = Get-SafeValue { [int]$ParagraphFormat.Alignment }
        left_indent_cm = Convert-PointsToCentimeters (Get-SafeValue { $ParagraphFormat.LeftIndent })
        right_indent_cm = Convert-PointsToCentimeters (Get-SafeValue { $ParagraphFormat.RightIndent })
        first_line_indent_cm = Convert-PointsToCentimeters (Get-SafeValue { $ParagraphFormat.FirstLineIndent })
        character_unit_left_indent = Get-SafeValue { [double]$ParagraphFormat.CharacterUnitLeftIndent }
        character_unit_right_indent = Get-SafeValue { [double]$ParagraphFormat.CharacterUnitRightIndent }
        character_unit_first_line_indent = Get-SafeValue { [double]$ParagraphFormat.CharacterUnitFirstLineIndent }
        space_before_pt = Convert-Points (Get-SafeValue { $ParagraphFormat.SpaceBefore })
        space_after_pt = Convert-Points (Get-SafeValue { $ParagraphFormat.SpaceAfter })
        line_spacing_rule = Get-SafeValue { [int]$ParagraphFormat.LineSpacingRule }
        line_spacing_pt = Convert-Points (Get-SafeValue { $ParagraphFormat.LineSpacing })
        keep_with_next = Convert-TriState (Get-SafeValue { $ParagraphFormat.KeepWithNext })
        keep_together = Convert-TriState (Get-SafeValue { $ParagraphFormat.KeepTogether })
        page_break_before = Convert-TriState (Get-SafeValue { $ParagraphFormat.PageBreakBefore })
        widow_control = Convert-TriState (Get-SafeValue { $ParagraphFormat.WidowControl })
        outline_level = Get-SafeValue { [int]$ParagraphFormat.OutlineLevel }
    }
}

function Get-CharacterSegments {
    param($Range)
    $text = Get-CleanText (Get-SafeValue { [string]$Range.Text } '')
    if ([string]::IsNullOrEmpty($text)) { return @() }
    return @([ordered]@{ text = $text; font = (Get-FontInfo $Range.Font) })
}

function Get-ParagraphInfo {
    param($Paragraph, [int]$Index)
    $range = $Paragraph.Range
    $styleName = Get-SafeValue { [string]$range.Style.NameLocal }
    if ([string]::IsNullOrWhiteSpace($styleName)) {
        $styleName = Get-SafeValue { [string]$range.Style }
    }
    $listFormat = Get-SafeValue { $range.ListFormat } $null
    $result = [ordered]@{
        index = $Index
        page = Get-SafeValue { [int]$range.Information(3) }
        story_type = Get-SafeValue { [int]$range.StoryType }
        in_table = Convert-TriState (Get-SafeValue { $range.Information(12) })
        text = Get-CleanText (Get-SafeValue { [string]$range.Text } '')
        style = $styleName
        font = Get-FontInfo $range.Font
        paragraph = Get-ParagraphFormatInfo $Paragraph.Format
        list_type = if ($null -ne $listFormat) { Get-SafeValue { [int]$listFormat.ListType } } else { $null }
        list_level = if ($null -ne $listFormat) { Get-SafeValue { [int]$listFormat.ListLevelNumber } } else { $null }
        character_segments = @(Get-CharacterSegments $range)
    }
    if ($null -ne $listFormat) { Release-ComObject $listFormat }
    Release-ComObject $range
    return $result
}

function Get-BorderInfo {
    param($Borders)
    $items = New-Object System.Collections.Generic.List[object]
    foreach ($index in @(-1, -2, -3, -4, -5, -6)) {
        $border = Get-SafeValue { $Borders.Item($index) } $null
        if ($null -ne $border) {
            $items.Add([ordered]@{
                index = $index
                line_style = Get-SafeValue { [int]$border.LineStyle }
                line_width = Get-SafeValue { [int]$border.LineWidth }
                color = Get-SafeValue { [int]$border.Color }
                visible = Convert-TriState (Get-SafeValue { $border.Visible })
            })
            Release-ComObject $border
        }
    }
    return $items.ToArray()
}

function Get-TableInfo {
    param($Table, [int]$Index)
    $range = $Table.Range
    $cells = New-Object System.Collections.Generic.List[object]
    $cellCollection = $Table.Range.Cells
    $cellCount = Get-SafeValue { [int]$cellCollection.Count } 0
    for ($i = 1; $i -le $cellCount; $i++) {
        $cell = $cellCollection.Item($i)
        $cellRange = $cell.Range
        $paragraphs = New-Object System.Collections.Generic.List[object]
        $pCount = Get-SafeValue { [int]$cellRange.Paragraphs.Count } 0
        for ($j = 1; $j -le $pCount; $j++) {
            $p = $cellRange.Paragraphs.Item($j)
            $paragraphs.Add((Get-ParagraphInfo $p $j))
            Release-ComObject $p
        }
        $cells.Add([ordered]@{
            index = $i
            row = Get-SafeValue { [int]$cell.RowIndex }
            column = Get-SafeValue { [int]$cell.ColumnIndex }
            width_cm = Convert-PointsToCentimeters (Get-SafeValue { $cell.Width })
            vertical_alignment = Get-SafeValue { [int]$cell.VerticalAlignment }
            shading_background_color = Get-SafeValue { [int]$cell.Shading.BackgroundPatternColor }
            text = Get-CleanText (Get-SafeValue { [string]$cellRange.Text } '')
            paragraphs = $paragraphs.ToArray()
        })
        Release-ComObject $cellRange
        Release-ComObject $cell
    }
    Release-ComObject $cellCollection

    $rows = New-Object System.Collections.Generic.List[object]
    $rowCount = Get-SafeValue { [int]$Table.Rows.Count } 0
    for ($i = 1; $i -le $rowCount; $i++) {
        $row = Get-SafeValue { $Table.Rows.Item($i) } $null
        if ($null -ne $row) {
            $rows.Add([ordered]@{
                index = $i
                height_cm = Convert-PointsToCentimeters (Get-SafeValue { $row.Height })
                height_rule = Get-SafeValue { [int]$row.HeightRule }
                heading_format = Convert-TriState (Get-SafeValue { $row.HeadingFormat })
                allow_break_across_pages = Convert-TriState (Get-SafeValue { $row.AllowBreakAcrossPages })
            })
            Release-ComObject $row
        }
    }

    $result = [ordered]@{
        index = $Index
        page = Get-SafeValue { [int]$range.Information(3) }
        rows = $rowCount
        columns = Get-SafeValue { [int]$Table.Columns.Count }
        alignment = Get-SafeValue { [int]$Table.Rows.Alignment }
        allow_autofit = Convert-TriState (Get-SafeValue { $Table.AllowAutoFit })
        preferred_width_type = Get-SafeValue { [int]$Table.PreferredWidthType }
        preferred_width = Get-SafeValue { [double]$Table.PreferredWidth }
        left_indent_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.Rows.LeftIndent })
        spacing_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.Spacing })
        top_padding_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.TopPadding })
        bottom_padding_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.BottomPadding })
        left_padding_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.LeftPadding })
        right_padding_cm = Convert-PointsToCentimeters (Get-SafeValue { $Table.RightPadding })
        borders = @(Get-BorderInfo $Table.Borders)
        row_settings = $rows.ToArray()
        cells = $cells.ToArray()
    }
    Release-ComObject $range
    return $result
}

function Get-HeaderFooterInfo {
    param($Section, [string]$Kind)
    $collection = if ($Kind -eq 'header') { $Section.Headers } else { $Section.Footers }
    $items = New-Object System.Collections.Generic.List[object]
    foreach ($index in @(1, 2, 3)) {
        $item = Get-SafeValue { $collection.Item($index) } $null
        if ($null -ne $item) {
            $range = $item.Range
            $items.Add([ordered]@{
                index = $index
                exists = Convert-TriState (Get-SafeValue { $item.Exists })
                link_to_previous = Convert-TriState (Get-SafeValue { $item.LinkToPrevious })
                text = Get-CleanText (Get-SafeValue { [string]$range.Text } '')
                font = Get-FontInfo $range.Font
                paragraph = Get-ParagraphFormatInfo $range.ParagraphFormat
                page_numbers_count = Get-SafeValue { [int]$item.PageNumbers.Count } 0
            })
            Release-ComObject $range
            Release-ComObject $item
        }
    }
    Release-ComObject $collection
    return $items.ToArray()
}

$source = (Resolve-Path -LiteralPath $SourceDirectory).Path
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$output = (Resolve-Path -LiteralPath $OutputDirectory).Path
$pdfDirectory = Join-Path $output 'pdfs'
$docxDirectory = Join-Path $output 'docx-copies'
New-Item -ItemType Directory -Path $pdfDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $docxDirectory -Force | Out-Null

$files = Get-ChildItem -LiteralPath $source -Filter '*.doc' | Sort-Object @{ Expression = { if ($_.Name -match '^附件(\d+)') { [int]$Matches[1] } else { 999 } } }
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$word.Options.UpdateLinksAtOpen = $false
$results = New-Object System.Collections.Generic.List[object]

try {
    foreach ($file in $files) {
        Write-Host "Processing $($file.Name)"
        $doc = $word.Documents.Open($file.FullName, $false, $true, $false)
        try {
            $doc.Repaginate()
            $baseName = [IO.Path]::GetFileNameWithoutExtension($file.Name)
            $pdfPath = Join-Path $pdfDirectory ($baseName + '.pdf')
            $docxPath = Join-Path $docxDirectory ($baseName + '.docx')
            $doc.ExportAsFixedFormat($pdfPath, 17)
            $doc.SaveAs2($docxPath, 16)

            $sections = New-Object System.Collections.Generic.List[object]
            $sectionCount = Get-SafeValue { [int]$doc.Sections.Count } 0
            for ($i = 1; $i -le $sectionCount; $i++) {
                $section = $doc.Sections.Item($i)
                $setup = $section.PageSetup
                $textColumns = Get-SafeValue { $setup.TextColumns } $null
                $sections.Add([ordered]@{
                    index = $i
                    start_page = Get-SafeValue { [int]$section.Range.Information(3) }
                    start_type = Get-SafeValue { [int]$setup.SectionStart }
                    orientation = Get-SafeValue { [int]$setup.Orientation }
                    page_width_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.PageWidth })
                    page_height_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.PageHeight })
                    top_margin_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.TopMargin })
                    bottom_margin_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.BottomMargin })
                    left_margin_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.LeftMargin })
                    right_margin_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.RightMargin })
                    gutter_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.Gutter })
                    header_distance_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.HeaderDistance })
                    footer_distance_cm = Convert-PointsToCentimeters (Get-SafeValue { $setup.FooterDistance })
                    mirror_margins = Convert-TriState (Get-SafeValue { $setup.MirrorMargins })
                    different_first_page = Convert-TriState (Get-SafeValue { $setup.DifferentFirstPageHeaderFooter })
                    odd_even_pages = Convert-TriState (Get-SafeValue { $setup.OddAndEvenPagesHeaderFooter })
                    vertical_alignment = Get-SafeValue { [int]$setup.VerticalAlignment }
                    columns_count = if ($null -ne $textColumns) { Get-SafeValue { [int]$textColumns.Count } } else { $null }
                    headers = @(Get-HeaderFooterInfo $section 'header')
                    footers = @(Get-HeaderFooterInfo $section 'footer')
                })
                if ($null -ne $textColumns) { Release-ComObject $textColumns }
                Release-ComObject $setup
                Release-ComObject $section
            }

            $paragraphs = New-Object System.Collections.Generic.List[object]
            $paragraphCount = Get-SafeValue { [int]$doc.Paragraphs.Count } 0
            for ($i = 1; $i -le $paragraphCount; $i++) {
                $p = $doc.Paragraphs.Item($i)
                $paragraphs.Add((Get-ParagraphInfo $p $i))
                Release-ComObject $p
            }

            $tables = New-Object System.Collections.Generic.List[object]
            $tableCount = Get-SafeValue { [int]$doc.Tables.Count } 0
            for ($i = 1; $i -le $tableCount; $i++) {
                $table = $doc.Tables.Item($i)
                $tables.Add((Get-TableInfo $table $i))
                Release-ComObject $table
            }

            $styles = New-Object System.Collections.Generic.List[object]
            $usedStyleNames = @($paragraphs | ForEach-Object { $_.style } | Where-Object { $_ } | Sort-Object -Unique)
            foreach ($styleName in $usedStyleNames) {
                $style = Get-SafeValue { $doc.Styles.Item($styleName) } $null
                if ($null -ne $style) {
                    $styles.Add([ordered]@{
                        name = $styleName
                        type = Get-SafeValue { [int]$style.Type }
                        base_style = Get-SafeValue { [string]$style.BaseStyle }
                        next_paragraph_style = Get-SafeValue { [string]$style.NextParagraphStyle }
                        font = Get-FontInfo $style.Font
                        paragraph = Get-ParagraphFormatInfo $style.ParagraphFormat
                    })
                    Release-ComObject $style
                }
            }

            $shapeItems = New-Object System.Collections.Generic.List[object]
            $shapeCount = Get-SafeValue { [int]$doc.Shapes.Count } 0
            for ($i = 1; $i -le $shapeCount; $i++) {
                $shape = $doc.Shapes.Item($i)
                $shapeText = ''
                if ((Get-SafeValue { [int]$shape.TextFrame.HasText } 0) -eq -1) {
                    $shapeText = Get-CleanText (Get-SafeValue { [string]$shape.TextFrame.TextRange.Text } '')
                }
                $shapeItems.Add([ordered]@{
                    index = $i
                    name = Get-SafeValue { [string]$shape.Name }
                    type = Get-SafeValue { [int]$shape.Type }
                    width_cm = Convert-PointsToCentimeters (Get-SafeValue { $shape.Width })
                    height_cm = Convert-PointsToCentimeters (Get-SafeValue { $shape.Height })
                    left_cm = Convert-PointsToCentimeters (Get-SafeValue { $shape.Left })
                    top_cm = Convert-PointsToCentimeters (Get-SafeValue { $shape.Top })
                    text = $shapeText
                })
                Release-ComObject $shape
            }

            $results.Add([ordered]@{
                attachment_number = if ($file.Name -match '^附件(\d+)') { [int]$Matches[1] } else { $null }
                source_path = $file.FullName
                file_name = $file.Name
                sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
                file_size_bytes = $file.Length
                page_count = Get-SafeValue { [int]$doc.ComputeStatistics(2) }
                section_count = $sectionCount
                paragraph_count = $paragraphCount
                table_count = $tableCount
                inline_shape_count = Get-SafeValue { [int]$doc.InlineShapes.Count } 0
                shape_count = $shapeCount
                pdf_path = $pdfPath
                docx_copy_path = $docxPath
                sections = $sections.ToArray()
                used_styles = $styles.ToArray()
                paragraphs = $paragraphs.ToArray()
                tables = $tables.ToArray()
                shapes = $shapeItems.ToArray()
            })
        }
        finally {
            $doc.Close(0)
            Release-ComObject $doc
        }
    }
}
finally {
    $word.Quit()
    Release-ComObject $word
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}

$jsonPath = Join-Path $output 'template-specs.json'
$results | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
Write-Host "Wrote $jsonPath"

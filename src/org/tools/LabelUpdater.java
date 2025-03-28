package org.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// @formatter:off
/**
 * 文件内容替换需求:
 * 当文件1中数据第二行的内容 等于 文件2中数据第二行 "label": 后""内数据时，
 * 用文件1中数据第三行的内容，替换匹配的文件2中数据第二行 "label": 后""中的内容.
 * 
 * 例如: * 
 * 文件1 内容格式(每个信息包含3行，每个信息间已空行分隔):
 * "
 * /ROM/FC/FC 001.nes
 * FC 001
 * 魂斗罗 一代 汉化版
 * 
 * /ROM/FC/FC 002.nes
 * FC 002
 * 超级魂斗罗  二代
 * 
 * ...
 * "
 * 文件2 内容格式(JSON格式，每个数据包含6行):
 * "
 *   {
 *     "path": "/storage/emulated/0/RetroArch/ROM/FC/FC 001.nes",
 *     "label": "FC 001",
 *     "core_path": "/data/user/0/com.retroarch.aarch64/cores/fceumm_libretro_android.so",
 *     "core_name": "Nintendo - NES / Famicom (FCEUmm)",
 *     "crc32": "00000000|crc",
 *     "db_name": "FC.lpl"
 *   },
 *   {
 *     "path": "/storage/emulated/0/RetroArch/ROM/FC/FC 002.nes",
 *     "label": "FC 002",
 *     "core_path": "DETECT",
 *     "core_name": "DETECT",
 *     "crc32": "00000000|crc",
 *     "db_name": "FC.lpl"
 *   },
 *   ...
 * "
 * 程序会将文件2中  "label": "FC 001",  行内容替换为 "label": "魂斗罗 一代 汉化版"，其它行不变。
 */

/**
 * 好的，我现在需要解决用户提出的问题，也就是用Java实现将文件1中的第三行内容替换到文件2对应的label中的功能。让我仔细看一下用户的需求。
 * 
 * 首先，用户的两个文件结构需要明确。文件1的每个条目有三行，中间有空行分隔。
 * 比如： 
 * /ROM/FC/FC 001.nes 
 * FC 001 
 * 魂斗罗 一代 汉化版
 * 
 * 然后是空行，接着下一个条目。每个条目的第二行是FC 001这样的标签，第三行是需要替换的新名称。
 * 
 * 文件2是JSON格式的数据，每个条目包含6行，其中第二行是"label": "FC 001",这样的结构。当文件1中的第二行和文件2中的label值匹配时，需要将文件2中的label值替换为文件1中的第三行内容。
 * 
 * 那首先，我需要如何解析这两个文件呢？
 * 
 * 对于文件1，我需要按三个行一组来读取，每组之间有空行。可能需要按空行分割成不同的块，然后每个块提取第二行作为键，第三行作为值。比如，建立一个Map，键是FC 001，对应的值是魂斗罗一代汉化版。
 * 
 * 处理文件1的时候，需要注意读取每一组的三行，跳过空行。可能需要按行读取，然后每次读取三个非空行作为一个条目，直到文件结束。
 * 
 * 对于文件2，虽然用户说是JSON格式，但看起来每个条目是手动格式化的，每行对应一个字段。用户提供的例子中，每个条目有六行，其中第二行是label的信息。但实际JSON应该是每个对象之间用逗号分隔，可能整个文件是一个数组的结构。不过用户提供的文件2示例最后有一个逗号，可能整个结构并不是严格的JSON数组。所以直接解析为JSON对象可能比较复杂，尤其是考虑到可能有格式上的不一致。
 * 
 * 或者，可能用户希望逐行处理文件2，当检测到某一行是"label": "xxx"时，检查该xxx是否存在于文件1的Map中，如果存在，则替换后面的内容。这种方法可能更简单，因为不需要完整的JSON解析，只需要处理特定行。
 * 
 * 这可能更有效率，特别是当文件较大时，逐行处理比解析整个JSON结构更节省资源。同时，用户提供的示例显示每个label行是固定的位置，即每个对象的第二行。所以，可能可以按每六行为一个块来处理？或者，或许更灵活的方式是，每当遇到包含"label":的行时，提取其值，然后替换。
 * 
 * 但具体来说，文件2的结构可能每个条目不一定严格是六行，或者可能存在其他字段的变化。因此，更可靠的方式是逐行读取文件2，当发现某一行是"label":开头的，那么提取其中的值，然后在Map中查找对应的替换值。如果找到，就替换该行的内容。
 * 
 * 这样处理的好处是不需要关心每个对象有多少行，只需要关注特定的行。比如，当处理到某一行包含"label":的时候，进行处理。
 * 
 * 所以，具体的步骤应该是：
 * 
 * 1. 读取文件1，建立Map，键是第二行的内容，值是对应的第三行的内容。
 * 2. 读取文件2，逐行处理，当遇到包含"label":的行时，提取当前label的值，检查是否在Map中存在。如果存在，就将该行的内容替换为新的label值。
 * 3. 将处理后的内容写入新的文件，或者覆盖原文件，但通常可能是生成一个新文件。 
 * 
 * 现在，如何处理文件1？
 * 
 * 读取文件1的时候，需要按三个行一组来处理，每组之间有一个空行。例如：
 * 
 * 行1：路径 
 * 行2：标签 
 * 行3：名称 
 * 空行 
 * 重复...
 * 
 * 所以，可以按如下的方式处理：
 * 
 * 使用一个循环，每次读取三个非空行，然后跳过空行。或者，将整个文件内容读入，按两个换行符分割成块（可能由于空行导致多个块之间有空字符串），然后每个块处理三个行。
 * 
 * 例如，在Java中，可以将文件内容读入一个字符串，然后用split("\n\n+")分割成各个块。然后每个块分割成行，取第二行作为键，第三行作为值。注意，每个块可能有三个行，但可能后面有空行或者其他情况？
 * 
 * 例如，块的内容是：
 * 
 * /ROM/FC/FC 001.nes\nFC 001\n魂斗罗 一代 汉化版
 * 
 * 所以，split("\n")之后得到三行，第一行是路径，第二行是标签，第三行是名称。这时候，可以取第二行的内容作为键，第三行的内容作为值。
 * 
 * 所以，处理文件1的大致流程：
 * 
 * 读取文件1的所有内容，分割成块（按两个或更多换行分隔），每个块再分割成行，取第二行的内容作为键，第三行的内容作为值，存入Map。
 * 
 * 然后处理文件2，逐行读取，当某一行包含"label":时，提取其中的标签，查找Map中是否存在对应的键。如果存在，就替换该行的标签值为Map中的值。
 * 
 * 如何提取当前标签的值？比如，行的格式是："label": "FC 001",。需要提取引号中的内容。可以使用正则表达式来匹配，例如：
 * 
 * Pattern pattern = Pattern.compile(""label": "(.*?)"");
 * 
 * 然后，找到匹配的组，得到原来的标签，比如FC 001，然后在Map中查找对应的新名称，然后重新生成该行的字符串，替换原来的标签。
 * 
 * 例如，将原行中的"label": "FC 001"替换为"label": "魂斗罗 一代 汉化版"。
 * 
 * 需要注意行中可能有空格，比如用户提供的文件2中的例子中，行的开头有空格，比如" {",
 * 里面的行前面有多个空格。所以在替换时，需要保留前面的空格和后面的逗号等符号。因此，正确的做法是，用正则表达式捕获整个"label"行，然后替换其中的内容。
 * 
 * 例如，假设行是这样的：
 * 
 * " "label": "FC 001","
 * 
 * 我们可以用正则表达式匹配整个"label": "(.*?)"部分，并替换其中的内容。
 * 
 * 因此，处理文件2的步骤如下：
 * 
 * 逐行读取文件2的内容，对每一行检查是否包含"label":。如果是，提取当前标签，查找Map中的替换值，如果有，则生成新的行，用替换值代替原标签。
 * 
 * 例如：
 * 
 * 原行： "label": "FC 001",
 * 
 * 替换后的行： "label": "魂斗罗 一代 汉化版",
 * 
 * 需要注意保持其他部分的格式不变，比如引号、逗号等。
 * 
 * 现在，代码的大致结构：
 * 
 * 1. 读取文件1，构建标签到名称的映射。 
 * 2. 读取文件2，逐行处理，替换符合条件的行。 
 * 3. 输出修改后的文件2内容。
 * 
 * 现在，具体实现需要考虑文件的读写方式，以及如何处理可能的异常。
 * 
 * Java实现步骤：
 * 
 * 首先，读取文件1的内容，按块分割：
 * 
 * 假设文件1的路径是file1.txt，使用Java的NIO或IO读取所有内容。
 * 
 * 例如：
 * 
 * String content1 = Files.readString(Paths.get("file1.txt"), StandardCharsets.UTF_8);
 * 
 * 然后，用split("\n\n+")分割成各个块：
 * 
 * String[] blocks = content1.split("\n\n+");
 * 
 * 然后，遍历每个块：
 * 
 * Map<String, String> labelMap = new HashMap<>();
 * 
 * for (String block : blocks) { 
 * 	String[] lines = block.split("\n"); 
 * 	if(lines.length >=3) { 
 * 		String label = lines[1].trim(); 
 * 		String name = lines[2].trim(); 
 * 		labelMap.put(label, name); 
 * 	} 
 * }
 * 
 * 这里需要注意每个块可能被分割成三个行，所以取lines[1]和lines[2]作为键值对。
 * 
 * 然后处理文件2：
 * 
 * 逐行读取文件2，对每一行检查是否是label行。如果是，提取标签，替换。
 * 
 * 例如：
 * 
 * List outputLines = new ArrayList<>();
 * 
 * Pattern labelPattern = Pattern.compile("("label": ")[^"]*(")");
 * 
 * try (BufferedReader reader = new BufferedReader(new FileReader("file2.txt")))
 * { String line; while ((line = reader.readLine()) != null) { if
 * (line.trim().startsWith(""label": ")) { Matcher matcher =
 * labelPattern.matcher(line); if (matcher.find()) { String oldLabel =
 * matcher.group(0); // 其实可能需要更精确的匹配，比如捕获组中的内容 // 或者，使用捕获组来获取标签值 // 例如，正则表达式是
 * ""label": "(.?)"" Pattern p = Pattern.compile(""label": "(.?)""); Matcher m =
 * p.matcher(line); if (m.find()) { String key = m.group(1); if
 * (labelMap.containsKey(key)) { String newLine =
 * line.replaceFirst(Pattern.quote(m.group(0)), ""label": "" + labelMap.get(key)
 * + """); outputLines.add(newLine); continue; } } } } outputLines.add(line); }
 * }
 * 
 * 这里可能有一个问题，当替换时，原来的行可能有其他部分被错误匹配。例如，正则表达式是否正确匹配整个label字段？
 * 
 * 比如，原行是：
 * 
 * "label": "FC 001",
 * 
 * 正则表达式应匹配"label": "FC 001"部分，而忽略后面的逗号或其他字符。或者，可以匹配到引号之间的内容，然后替换。
 * 
 * 所以，正确的正则表达式可能应该捕获整个"label": "..."的部分，然后替换其中的内容。
 * 
 * 例如，正则表达式可以是：
 * 
 * (\s*"label": ")([^"])(",?.)
 * 
 * 这样，分成三个组：前面的空格和label部分，中间的值，以及后面的逗号和其他内容。然后，替换时，保持前后部分，只替换中间的值。
 * 
 * 或者，可能更简单的方式是使用split来提取标签的值。例如，将行分割成"label": "和后面的部分，然后取第一个"作为分隔符。
 * 
 * 或者，使用更精确的正则表达式：
 * 
 * "label":\s*"([^"]+)"
 * 
 * 这个正则表达式匹配"label": 后面可能有空格，然后引号内的内容，直到下一个引号。
 * 
 * 例如：
 * 
 * Pattern p = Pattern.compile(""label":\s*"([^"]+)"");
 * 
 * 然后，当找到匹配时，取出组1的值，即原来的标签，然后在Map中查找替换。
 * 
 * 找到匹配后，可以用replace方法将原标签替换为新值。
 * 
 * 例如：
 * 
 * String key = m.group(1); String replacement = ""label": "" +
 * labelMap.get(key) + """;
 * 
 * 然后，将原行中的匹配部分替换为replacement。
 * 
 * 这应该可以保持其他格式不变，例如逗号等。
 * 
 * 因此，修改后的处理逻辑：
 * 
 * 对于每一行，检查是否包含"label":，然后提取当前标签，查找Map，如果存在，则替换。
 * 
 * 具体代码：
 * 
 * Pattern labelPattern = Pattern.compile(""label":\s*"([^"]*)"");
 * 
 * for (String line : linesFromFile2) { Matcher matcher =
 * labelPattern.matcher(line); if (matcher.find()) { String currentLabel =
 * matcher.group(1); if (labelMap.containsKey(currentLabel)) { String newLabel =
 * labelMap.get(currentLabel); line = matcher.replaceFirst(""label": "" +
 * newLabel.replace(""", "\"") + """); } } outputLines.add(line); }
 * 
 * 这里需要注意转义，比如如果newLabel中包含双引号，会导致JSON格式错误。不过根据用户提供的例子，文件1中的第三行可能不包含双引号，但为了安全起见，可能需要将双引号转义。例如，替换成"。但在Java字符串中，需要使用两个反斜杠。
 * 
 * 不过用户的需求中，文件1第三行的例子是“魂斗罗 一代
 * 汉化版”，没有双引号，所以暂时可能不需要处理。但为了代码的鲁棒性，可能需要考虑转义。或者，用户可能希望确保替换后的字符串中的双引号被正确转义，但根据问题描述，用户没有提到这点，可能假设文件1中的第三行不含双引号，所以可以暂时不考虑。
 * 
 * 综上，Java代码的大致流程是：
 * 
 * 读取文件1，建立映射。 读取文件2，逐行处理，找到label行进行替换。 输出修改后的内容到新文件或控制台。 现在，考虑编写完整的Java代码。
 * 
 * 首先，处理文件1：
 * 
 * 读取文件1的路径，假设是args[0]，文件2的路径是args[1]，输出到args[2]或直接修改原文件。或者，根据用户的要求，可能需要将处理后的结果输出到某个位置。这里假设用户希望读取两个文件，并输出处理后的文件2内容到标准输出或另一个文件。
 * 
 * 这里可能需要处理文件输入输出的异常，比如IOException，以及文件路径的问题。
 * 
 * 接下来，编写完整的Java类：
 * 
 * import java.io.*; import java.nio.charset.StandardCharsets; import
 * java.nio.file.Files; import java.nio.file.Paths; import java.util.HashMap;
 * import java.util.List; import java.util.ArrayList; import
 * java.util.regex.Pattern; import java.util.regex.Matcher; import
 * java.util.stream.Collectors;
 * 
 * public class FileLabelUpdater {
 * 
 * public static void main(String[] args) { if (args.length < 2) {
 * System.out.println("Usage: java FileLabelUpdater <file1> <file2>
 * [outputFile]"); return; }
 * 
 * String file1Path = args[0]; String file2Path = args[1]; String outputPath =
 * args.length >= 3 ? args[2] : file2Path;
 * 
 * try { // 读取文件1，构建label映射 HashMap<String, String> labelMap =
 * readFile1(file1Path);
 * 
 * // 处理文件2，替换label List<String> updatedLines = processFile2(file2Path,
 * labelMap);
 * 
 * // 写入输出文件 Files.write(Paths.get(outputPath), updatedLines,
 * StandardCharsets.UTF_8); System.out.println("处理完成！输出文件：" + outputPath);
 * 
 * } catch (IOException e) { e.printStackTrace(); } }
 * 
 * private static HashMap<String, String> readFile1(String filePath) throws
 * IOException { HashMap<String, String> labelMap = new HashMap<>(); String
 * content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
 * String[] blocks = content.split("\\n\\n+"); // 分割成块，每个块由空行分隔
 * 
 * for (String block : blocks) { String[] lines = block.split("\\n"); if
 * (lines.length >= 3) { String label = lines[1].trim(); String name =
 * lines[2].trim(); labelMap.put(label, name); } }
 * 
 * return labelMap; }
 * 
 * private static List<String> processFile2(String filePath, HashMap<String,
 * String> labelMap) throws IOException { List<String> lines =
 * Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8); Pattern
 * labelPattern = Pattern.compile("\"label\":\\s*\"([^\"]*)\"");
 * 
 * return lines.stream().map(line -> { Matcher matcher =
 * labelPattern.matcher(line); if (matcher.find()) { String currentLabel =
 * matcher.group(1); if (labelMap.containsKey(currentLabel)) { String newLabel =
 * labelMap.get(currentLabel); // 替换当前label的值 return
 * matcher.replaceFirst("\"label\": \"" + newLabel + "\""); } } return line;
 * }).collect(Collectors.toList()); } }
 * 
 * 需要注意的问题：
 * 
 * 文件1的分割是否正确？比如，如果文件1中的块之间有一个或多个空行，split("\n\n+")会正确分割。但假设文件使用不同的换行符，比如Windows的\r\n，可能导致split错误。这时可能需要将split的参数改为"\r?\n\r?\n+"，或者在读取文件时统一处理换行符。
 * 但Files.readString在读取文件时，会将换行符转换为系统的默认换行符？或者保持原样？可能需要注意，但假设用户提供的文件使用\n作为换行符。
 * 
 * 处理文件2的替换是否正确？正则表达式是否正确匹配了所有可能的格式？例如，可能存在空格，如"label" : "FC
 * 001"（冒号前后有空格），这时候正则表达式中的\s*可以匹配任意数量的空格。
 * 
 * 替换后的行是否保留了原有的格式，比如逗号？比如，原行是"label": "FC 001",，替换后的行应该是"label":
 * "魂斗罗...",。正则表达式中的匹配是到引号结束，所以replaceFirst会将整个匹配的部分替换，包括引号内的内容，但保留外面的逗号。例如，原来的字符串是"label":
 * "FC 001",，正则表达式匹配的是"label": "FC 001"，替换为"label": "新值"，所以行变成"label":
 * "新值",，这符合要求。
 * 
 * 如何处理转义字符？如果文件1中的第三行包含双引号或者反斜杠，可能会导致JSON格式错误。但根据用户的需求，可能假设文件1中的名称是纯文本，没有特殊字符。如果需要处理，可以在替换时对newLabel进行转义处理，比如将"替换为"。例如，在替换时，newLabel
 * = newLabel.replace(""", "\"");。这可能需要添加到代码中。
 * 
 * 例如：
 * 
 * String newLabel = labelMap.get(currentLabel).replace(""", "\"");
 * 
 * 或者在替换时处理：
 * 
 * return matcher.replaceFirst(""label": "" + newLabel.replace(""", "\"") +
 * """);
 * 
 * 但根据用户提供的例子，似乎不需要。但为了代码的鲁棒性，可能应该添加。例如：
 * 
 * 在processFile2方法中的替换部分：
 * 
 * String escapedLabel = newLabel.replace(""", "\\""); // 转义双引号为" return
 * matcher.replaceFirst(""label": "" + escapedLabel + """);
 * 
 * 这样，如果newLabel中有双引号，会被转义。比如，假设名称是魂斗罗"一代，那么会被转义为魂斗罗"一代，从而保持JSON的正确性。
 * 
 * 但用户提供的示例中没有这种情况，但可能在实际情况中出现。所以，建议加上转义处理。
 * 
 * 修改后的代码：
 * 
 * String newLabel = labelMap.get(currentLabel); String escapedLabel =
 * newLabel.replace(""", "\\""); return matcher.replaceFirst(""label": "" +
 * escapedLabel + """);
 * 
 * 这样，可以处理双引号的问题。
 * 
 * 另外，在Java中，反斜杠需要使用两个，所以如果要替换成"，需要写成"\\""，因为Java字符串中的每个反斜杠都需要转义。
 * 
 * 综上，最终的processFile2方法中的部分代码应修改为：
 * 
 * if (labelMap.containsKey(currentLabel)) { String newLabel =
 * labelMap.get(currentLabel); // 转义双引号 String escapedLabel =
 * newLabel.replace(""", "\\""); // 替换 return matcher.replaceFirst(""label": ""
 * + escapedLabel + """); }
 * 
 * 现在，测试用户提供的例子是否正确：
 * 
 * 文件1中的条目：
 * 
 * FC 001 → 魂斗罗 一代 汉化版
 * 
 * 文件2中的对应行：
 * 
 * "label": "FC 001",
 * 
 * 处理后应该变成：
 * 
 * "label": "魂斗罗 一代 汉化版",
 * 
 * 代码中的正则表达式会匹配到"label": "FC 001"，并替换为新的值，包括双引号内的内容。同时，后面的逗号被保留下来。
 * 
 * 现在，关于换行符的问题，Java的readAllLines方法会自动处理不同平台的换行符，所以在处理文件时，不管原文件是Windows还是Linux格式，都能正确分割行。同样，写入时使用系统的默认换行符，或者可以指定。
 * 
 * 综上，上述代码应该能够满足用户的需求。
 */
//@formatter:on

public class LabelUpdater {

	public static void main(String[] args) throws IOException {
//		if (args.length < 2) {
//			System.out.println("用法: java LabelUpdater 文件1路径 文件2路径 [输出文件路径]");
//			return;
//		}
//		HashMap<String, String> labelMap = buildLabelMap(args[0]);
//		List<String> updatedLines = processFile(args[1], labelMap);
//		String outputPath = args.length > 2 ? args[2] : args[1];

		String file1 = "/home/tianfeng3/1.lpl";
		String file2 = "/home/tianfeng3/2.lpl";
		HashMap<String, String> labelMap = buildLabelMap(file1);
		List<String> updatedLines = processFile(file2, labelMap);
		String outputPath = file2;

		Files.write(Paths.get(outputPath), updatedLines, StandardCharsets.UTF_8);
		System.out.println("文件更新完成，输出路径: " + outputPath);
	}

	private static HashMap<String, String> buildLabelMap(String filePath) throws IOException {
		HashMap<String, String> map = new HashMap<>();
		String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

		// 分割不同游戏条目
		String[] entries = content.split("\\n\\n"); // content.split("\\r\\n\\r\\n"); // content.split("\\r\\n+");

		for (String entry : entries) {
			String[] lines = entry.split("\\n"); // entry.split("\\r\\n"); // entry.split("\\n");
			if (lines.length >= 3) {
				String label = lines[1].trim();
				String name = lines[2].trim();
				map.put(label, name);
			}
		}
		return map;
	}

	private static List<String> processFile(String filePath, HashMap<String, String> labelMap) throws IOException {
		Pattern pattern = Pattern.compile("\"label\":\\s*\"([^\"]*)\"");

		List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

		return lines.stream().map(line -> {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				String currentLabel = matcher.group(1);
				if (labelMap.containsKey(currentLabel)) {
					String newName = labelMap.get(currentLabel).replace("\\", "\\\\") // 处理反斜杠
							.replace("\"", "\\\""); // 处理双引号
					return line.replaceFirst(Pattern.quote(matcher.group(0)), "\"label\": \"" + newName + "\"");
				}
			}
			return line;
		}).collect(Collectors.toList());
	}
}

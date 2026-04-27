package vn.viettel.vds.promotion.validation.application.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a rule node tree into Drools DRL text.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>For each COND leaf: load template from {@code rule-templates/{compilerId}.drl.mustache},
 *       render with node {@code params} using Handlebars/Mustache.</li>
 *   <li>For GROUP AND: join child snippets with {@code &&}.</li>
 *   <li>For GROUP OR:  join child snippets with {@code ||}.</li>
 *   <li>For GROUP NONE: wrap the OR-joined children with {@code !(…)}.</li>
 * </ol>
 *
 * <p>Template files live in {@code src/main/resources/rule-templates/}
 * and follow the naming convention {@code {compilerId}.drl.mustache}.
 */
@Component
public class DrlCompiler {

    private static final Logger log = LoggerFactory.getLogger(DrlCompiler.class);

    private final Handlebars handlebars;

    public DrlCompiler() {
        ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/rule-templates", ".drl.mustache");
        this.handlebars = new Handlebars(loader);
    }

    /**
     * Compile a rule tree into DRL text.
     *
     * @param rule      rule aggregate (provides id and root logic)
     * @param nodes     tree-structured nodes (first element is root)
     * @param operators map of operatorName → Operator (for compilerId lookup)
     * @return DRL snippet string ready to be wrapped in a Drools rule block
     * @throws DrlCompileException if a template cannot be found or rendered
     */
    public String compile(Rule rule, List<RuleNode> nodes, Map<String, Operator> operators) {
        log.debug("Compiling DRL for ruleId={}", rule.getId());

        if (nodes == null || nodes.isEmpty()) {
            throw new DrlCompileException("Rule " + rule.getId() + " has no nodes to compile");
        }

        RuleNode root = nodes.get(0);
        String lhsSnippet = compileNode(root, operators);

        String drl = buildDrlRule(rule.getId(), lhsSnippet);
        log.debug("DRL compiled for ruleId={}, length={}", rule.getId(), drl.length());
        return drl;
    }

    // ---------- public for tests (including sub-packages) ----------

    /**
     * Render a single COND node's template snippet.
     *
     * <p>For templates that require computed boolean context flags (e.g.
     * {@code tpl_binding_validity_window_v1}), the render context is enriched
     * automatically before passing to Handlebars — see {@link #buildRenderContext}.
     */
    public String renderCondTemplate(String compilerId, Map<String, Object> params) {
        try {
            Template template = handlebars.compile(compilerId);
            Map<String, Object> ctx = buildRenderContext(compilerId, params);
            return template.apply(ctx).trim();
        } catch (IOException e) {
            throw new DrlCompileException(
                    "Cannot load or render DRL template for compilerId=" + compilerId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Build the full Handlebars render context for a given compilerId.
     *
     * <p>For most templates the context equals the raw {@code params}. Templates that
     * rely on computed boolean flags (section guards) receive additional keys derived
     * from the raw params here, keeping template logic pure Mustache (no helpers needed).
     *
     * <p>Currently enriched templates:
     * <ul>
     *   <li>{@code tpl_binding_validity_window_v1} — adds {@code hasStart}, {@code hasEnd},
     *       {@code both}, {@code timezone} derived from {@code startDate} / {@code endDate}.</li>
     * </ul>
     */
    private Map<String, Object> buildRenderContext(String compilerId, Map<String, Object> params) {
        Map<String, Object> ctx = new HashMap<>(params != null ? params : Map.of());
        if ("tpl_binding_validity_window_v1".equals(compilerId)) {
            Object startDate = ctx.get("startDate");
            Object endDate = ctx.get("endDate");
            ctx.put("hasStart", startDate != null);
            ctx.put("hasEnd", endDate != null);
            ctx.put("both", startDate != null && endDate != null);
            ctx.computeIfAbsent("timezone", k -> "Asia/Ho_Chi_Minh");
        }
        return ctx;
    }

    // ---------- private helpers ----------

    private String compileNode(RuleNode node, Map<String, Operator> operators) {
        if (node.getType() == RuleNode.NodeType.COND) {
            return compileCondNode(node, operators);
        }
        return compileGroupNode(node, operators);
    }

    private String compileCondNode(RuleNode node, Map<String, Operator> operators) {
        String operatorName = node.getOperatorName();
        Operator operator = operators != null ? operators.get(operatorName) : null;

        String compilerId;
        if (operator != null && operator.getCompilerId() != null) {
            compilerId = operator.getCompilerId();
        } else {
            // Fallback: derive compilerId from operatorName using naming convention tpl_{name}_v1
            compilerId = "tpl_" + operatorName.replace(".", "_") + "_v1";
            log.warn("No compilerId for operator={}, using fallback={}", operatorName, compilerId);
        }

        return renderCondTemplate(compilerId, node.getParams());
    }

    private String compileGroupNode(RuleNode node, Map<String, Operator> operators) {
        Rule.LogicType groupLogic = node.getGroupLogic();
        List<RuleNode> children = node.getChildren();

        if (children == null || children.isEmpty()) {
            throw new DrlCompileException("GROUP node id=" + node.getId() + " has no children");
        }

        List<String> childSnippets = new ArrayList<>();
        for (RuleNode child : children) {
            childSnippets.add(compileNode(child, operators));
        }

        return switch (groupLogic) {
            case ALL -> joinWithAnd(childSnippets);
            case ANY -> joinWithOr(childSnippets);
            case NONE -> negate(joinWithOr(childSnippets));
            case XOR -> joinWithXor(childSnippets);
        };
    }

    private String joinWithAnd(List<String> snippets) {
        if (snippets.size() == 1) {
            return "(" + snippets.get(0) + ")";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < snippets.size(); i++) {
            sb.append("(").append(snippets.get(i)).append(")");
            if (i < snippets.size() - 1) {
                sb.append(" && ");
            }
        }
        return sb.toString();
    }

    private String joinWithOr(List<String> snippets) {
        if (snippets.size() == 1) {
            return "(" + snippets.get(0) + ")";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < snippets.size(); i++) {
            sb.append("(").append(snippets.get(i)).append(")");
            if (i < snippets.size() - 1) {
                sb.append(" || ");
            }
        }
        return sb.toString();
    }

    private String negate(String expr) {
        return "!(" + expr + ")";
    }

    private String joinWithXor(List<String> snippets) {
        // XOR: exactly one true — represented as OR (approximate for DRL MVP)
        return joinWithOr(snippets);
    }

    /**
     * Wrap the LHS snippet in a complete DRL rule block.
     */
    private String buildDrlRule(String ruleId, String lhsSnippet) {
        return "package vn.viettel.vds.promotion.rules;\n\n" +
                "import vn.viettel.vds.promotion.rules.facts.*;\n\n" +
                "rule \"" + escapeQuotes(ruleId) + "\"\n" +
                "  when\n" +
                "    " + lhsSnippet + "\n" +
                "  then\n" +
                "    // rule matched\n" +
                "end\n";
    }

    private String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

    /**
     * Exception thrown when DRL compilation fails.
     */
    public static class DrlCompileException extends RuntimeException {
        public DrlCompileException(String message) {
            super(message);
        }

        public DrlCompileException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.page;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getFirstSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getLastSentenceInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getNextPageFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getNumberOfPages;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectSentenceAt;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action.ActionContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.exception.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationLayersModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CuratorUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the Automation page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the annotation pane to trigger automation on the lower
 * pane.
 */
@MountPath("/automation.html")
public class AutomationPage
    extends ApplicationPageBase
{
    private static final Log LOG = LogFactory.getLog(AutomationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "automationService")
    private AutomationService automationService;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private CurationContainer curationContainer;
    private ActionContext bModel;

    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;

    private int sentenceNumber = 1;
    private int totalNumberOfSentence;

    private long currentDocumentId;
    private long currentprojectId;

    // Open the dialog window on first load
    boolean firstLoad = true;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress;
    private AnnotationDetailEditorPanel editor;

    private FinishImage finish;

    private SuggestionViewPanel automateView;
    private BratAnnotator annotator;

    private final Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private final SourceListView curationSegment = new SourceListView();

    public AutomationPage()
    {
        bModel = new ActionContext();
        bModel.setMode(Mode.AUTOMATION);

        WebMarkupContainer sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", bModel.getPreferences().getSidebarSize()+"%");
            }
        };
        add(sidebarCell);

        WebMarkupContainer annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                aTag.put("width", (100-bModel.getPreferences().getSidebarSize())+"%");
            }
        };
        add(annotationViewCell);
        
        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bModel.getDocument() != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        automateView = new SuggestionViewPanel("automateView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curation segment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    curationContainer.setBratAnnotatorModel(bModel);
                    setCurationSegmentBeginEnd();

                    CuratorUtil.updatePanel(aTarget, this, curationContainer, annotator,
                            repository, annotationSelectionByUsernameAndAddress, curationSegment,
                            annotationService, userRepository);
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
                annotator.bratRenderLater(aTarget);
                aTarget.add(numberOfPages);
                update(aTarget);
            }
        };
        automateView.setOutputMarkupId(true);
        annotationViewCell.add(automateView);

        editor = new AnnotationDetailEditorPanel(
                "annotationDetailEditorPanel", new Model<ActionContext>(bModel))
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);

                try {
                    annotator.bratRender(aTarget, getCas(aBModel));
                }
                catch (UIMAException | ClassNotFoundException | IOException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }

                annotator.bratSetHighlight(aTarget, aBModel.getSelection().getAnnotation());

                annotator.onChange(aTarget, aBModel);
            }
            
            @Override
            public void onAnnotate(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
            	if(aBModel.isForwardAnnotation()){
            		return;
            	}
                AnnotationLayer layer = aBModel.getSelectedAnnotationLayer();
                int address = aBModel.getSelection().getAnnotation().getId();
                try {
                    AnnotationDocument annodoc = repository.createOrGetAnnotationDocument(
                            aBModel.getDocument(), aBModel.getUser());
                    JCas jCas = repository.readAnnotationCas(annodoc);
                    AnnotationFS fs = selectByAddr(jCas, address);

                    for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                        Type type = CasUtil.getType(fs.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (!automationService.existsMiraTemplate(f)) {
                            continue;
                        }
                        if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                            continue;
                        }
                        TagSet tagSet = f.getTagset();
                        boolean isRepeatable = false;
                        // repeat only if the value is in the tagset
                        for (Tag tag : annotationService.listTags(tagSet)) {
                            if (fs.getFeatureValueAsString(feat) == null) {
                                break; // this is new annotation without values
                            }
                            if (fs.getFeatureValueAsString(feat).equals(tag.getName())) {
                                isRepeatable = true;
                                break;
                            }
                        }
                        if (automationService.getMiraTemplate(f) != null && isRepeatable) {

                            if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {                               
                                AutomationUtil.repeateRelationAnnotation(aBModel, repository,
                                        annotationService, fs, f, fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }
                            else if (layer.getType().endsWith(WebAnnoConst.SPAN_TYPE)) {
                                AutomationUtil.repeateSpanAnnotation(aBModel, repository,
                                        annotationService, fs.getBegin(), fs.getEnd(), f,
                                        fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }

                        }
                    }
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget, ActionContext aBModel)
            {
                try {
                    annotator.autoForward(aTarget, getCas(aBModel));
                   // onAnnotate(aTarget, aBModel);
                }
                catch (UIMAException | ClassNotFoundException | IOException | BratAnnotationException e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }
            }
            
            @Override
            public void onDelete(AjaxRequestTarget aTarget, ActionContext aBModel,
                    AnnotationFS aFS)
            {
                AnnotationLayer layer = aBModel.getSelectedAnnotationLayer();
                for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                    if (!automationService.existsMiraTemplate(f)) {
                        continue;
                    }
                    if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                        continue;
                    }
                    try {
                        Type type = CasUtil.getType(aFS.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {
                            AutomationUtil.deleteRelationAnnotation(aBModel, repository,
                                    annotationService, aFS, f, aFS.getFeatureValueAsString(feat));
                        }
                        else {
                            AutomationUtil.deleteSpanAnnotation(aBModel, repository,
                                    annotationService, aFS.getBegin(), aFS.getEnd(), f,
                                    aFS.getFeatureValueAsString(feat));
                        }
                        update(aTarget);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }

            }
        };
        editor.setOutputMarkupId(true);
        sidebarCell.add(editor);

        annotator = new BratAnnotator("mergeView", new Model<ActionContext>(bModel),
                editor)
        {
            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            public void onChange(AjaxRequestTarget aTarget, ActionContext aBratAnnotatorModel)
            {
                try {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    // info(bratAnnotatorModel.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    bModel = aBratAnnotatorModel;
                    SuggestionBuilder builder = new SuggestionBuilder(repository,
                            annotationService, userRepository);
                    curationContainer = builder.buildCurationContainer(bModel);
                    setCurationSegmentBeginEnd();
                    curationContainer.setBratAnnotatorModel(bModel);

                    CuratorUtil.updatePanel(aTarget, automateView, curationContainer, this,
                            repository, annotationSelectionByUsernameAndAddress, curationSegment,
                            annotationService, userRepository);
                    aTarget.add(automateView);
                    aTarget.add(numberOfPages);
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
                update(aTarget);
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        annotator.setOutputMarkupId(true);
        annotationViewCell.add(annotator);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(bModel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel",
                new Model<ActionContext>(bModel)));

        add(numberOfPages = (Label) new Label("numberOfPages",
                new LoadableDetachableModel<String>()
                {
                    private static final long serialVersionUID = 891566759811286173L;

                    @Override
                    protected String load()
                    {
                        if (bModel.getDocument() != null) {

                            JCas mergeJCas = null;
                            try {

                                mergeJCas = repository.readCorrectionCas(bModel.getDocument());

                                totalNumberOfSentence = getNumberOfPages(mergeJCas);

                                // If only one page, start displaying from sentence 1
                                /*
                                 * if (totalNumberOfSentence == 1) {
                                 * bratAnnotatorModel.setSentenceAddress(bratAnnotatorModel
                                 * .getFirstSentenceAddress()); }
                                 */
                                List<SourceDocument> listofDoc = getListOfDocs();
                            	
                            	int docIndex = listofDoc.indexOf(bModel.getDocument())+1;
                            	
                                int address = getAddr(selectSentenceAt(mergeJCas,
                                        bModel.getSentenceBeginOffset(),
                                        bModel.getSentenceEndOffset()));
                                sentenceNumber = getFirstSentenceNumber(mergeJCas, address);
                                int firstSentenceNumber = sentenceNumber + 1;
                                int lastSentenceNumber;
                                if (firstSentenceNumber + bModel.getPreferences().getWindowSize()
                                        - 1 < totalNumberOfSentence) {
                                    lastSentenceNumber = firstSentenceNumber
                                            + bModel.getPreferences().getWindowSize() - 1;
                                }
                                else {
                                    lastSentenceNumber = totalNumberOfSentence;
                                }

                                return "showing " + firstSentenceNumber + "-" + lastSentenceNumber
                                        + " of " + totalNumberOfSentence + " sentences [document "
                                        + docIndex +" of "+ listofDoc.size()+"]";
                            }
                            catch (UIMAException e) {
                                return "";
                            }
                            catch (DataRetrievalFailureException e) {
                                return "";
                            }
                            catch (ClassNotFoundException e) {
                                return "";
                            }
                            catch (FileNotFoundException e) {
                                return "";
                            }
                            catch (IOException e) {
                                return "";
                            }

                        }
                        else {
                            return "";// no document yet selected
                        }

                    }
                }).setOutputMarkupId(true));

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
            	 bModel.getSelection().clear();
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), bModel, openDocumentsModal, Mode.AUTOMATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        if (bModel.getDocument() == null) {
                            setResponsePage(getApplication().getHomePage());
                            return;
                        }

                        try {
                            target.addChildren(getPage(), FeedbackPanel.class);
                            bModel.setDocument(bModel.getDocument());
                            bModel.setProject(bModel.getProject());

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            
                            loadDocumentAction(target);
                            setCurationSegmentBeginEnd();
                            update(target);
                            User user = userRepository.get(username);
                            editor.setEnabled(!FinishImage.isFinished(
                                    new Model<ActionContext>(bModel), user, repository));
    						editor.refresh(target);
    		
                        }
                        catch (UIMAException e) {
                            target.addChildren(getPage(), FeedbackPanel.class);
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (ClassNotFoundException e) {
                            target.addChildren(getPage(), FeedbackPanel.class);
                            error(e.getMessage());
                        }
                        catch (IOException e) {
                            target.addChildren(getPage(), FeedbackPanel.class);
                            error(e.getMessage());
                        }
                        catch (BratAnnotationException e) {
                            error(e.getMessage());
                        }
                        finish.setModelObject(bModel);
                        target.add(finish.setOutputMarkupId(true));
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                        target.add(documentNamePanel.setOutputMarkupId(true));
                        target.add(numberOfPages);
                    }
                });
                openDocumentsModal.show(aTarget);
            }
        });

        add(new AnnotationLayersModalPanel("annotationLayersModalPanel",
                new Model<ActionContext>(bModel), editor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                // Re-render the whole page because the width of the sidebar may have changed
                aTarget.add(AutomationPage.this);
                
                curationContainer.setBratAnnotatorModel(bModel);
                try {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    setCurationSegmentBeginEnd();
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                update(aTarget);
                // mergeVisualizer.reloadContent(aTarget);
                aTarget.appendJavaScript("Wicket.Window.unloadConfirmation = false;window.location.reload()");

            }
        });

        add(new ExportModalPanel("exportModalPanel", new Model<ActionContext>(bModel)){
            private static final long serialVersionUID = -468896211970839443L;
            
            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }
            
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(bModel.getProject() != null
                        && (SecurityUtil.isAdmin(bModel.getProject(), repository, bModel.getUser())
                                || !bModel.getProject().isDisableExport()));
            }
        });

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormSubmitBehavior(gotoPageTextFieldForm, "submit")
        {
            private static final long serialVersionUID = -4549805321484461545L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                if (gotoPageAddress == 0) {
                    aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                    return;
                }
                JCas mergeJCas = null;
                try {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                    if (bModel.getSentenceAddress() != gotoPageAddress) {

                        updateSentenceNumber(mergeJCas, gotoPageAddress);

                        SuggestionBuilder builder = new SuggestionBuilder(repository,
                                annotationService, userRepository);
                        curationContainer = builder.buildCurationContainer(bModel);
                        setCurationSegmentBeginEnd();
                        curationContainer.setBratAnnotatorModel(bModel);
                        update(aTarget);
                        annotator.bratRenderLater(aTarget);
                    }
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }
        });

        gotoPageTextField.setType(Integer.class);
        gotoPageTextField.setMinimum(1);
        gotoPageTextField.setDefaultModelObject(1);
        add(gotoPageTextFieldForm.add(gotoPageTextField));
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -3853194405966729661L;

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                JCas mergeJCas = null;
                try {
                    mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                    gotoPageAddress = getSentenceAddress(mergeJCas,
                            gotoPageTextField.getModelObject());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
                }

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {

                if (gotoPageAddress == 0) {
                    aTarget.appendJavaScript("alert('The sentence number entered is not valid')");
                    return;
                }
                if (bModel.getDocument() == null) {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                    return;
                }
                JCas mergeJCas = null;
                try {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                    if (bModel.getSentenceAddress() != gotoPageAddress) {

                        updateSentenceNumber(mergeJCas, gotoPageAddress);

                        SuggestionBuilder builder = new SuggestionBuilder(repository,
                                annotationService, userRepository);
                        curationContainer = builder.buildCurationContainer(bModel);
                        setCurationSegmentBeginEnd();
                        curationContainer.setBratAnnotatorModel(bModel);
                        update(aTarget);
                        annotator.bratRenderLater(aTarget);
                    }
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(e.getMessage());
                }
                catch (IOException e) {
                    error(e.getMessage());
                }
                catch (BratAnnotationException e) {
                    error(e.getMessage());
                }
            }
        });

        finish = new FinishImage("finishImage", new LoadableDetachableModel<ActionContext>()
        {
            private static final long serialVersionUID = -2737326878793568454L;

            @Override
            protected ActionContext load()
            {
                return bModel;
            }
        });

        add(new FinishLink("showYesNoModalPanel", new Model<ActionContext>(bModel), finish)
        {
            private static final long serialVersionUID = -4657965743173979437L;
            
            @Override
            public void onClose(AjaxRequestTarget aTarget)
            {
                super.onClose(aTarget);
                aTarget.add(editor);
            }
        });

        // Show the previous document, if exist
        add(new AjaxLink<Void>("showPreviousDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                editor.reset(aTarget);
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements=   getListOfDocs();

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    aTarget.appendJavaScript("alert('This is the first document!')");
                }
                else {
                    bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex - 1));

                    try {
                        repository.upgradeCasAndSave(bModel.getDocument(), Mode.AUTOMATION, bModel
                                .getUser().getUsername());
                        loadDocumentAction(aTarget);
                        setCurationSegmentBeginEnd();
                        update(aTarget);

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (IOException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (BratAnnotationException e) {
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        error(e.getMessage());
                    }

                    finish.setModelObject(bModel);
                    aTarget.add(finish.setOutputMarkupId(true));
                    aTarget.add(numberOfPages);
                    aTarget.add(documentNamePanel);
                    annotator.bratRenderLater(aTarget);
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up }, EventType.click)));

        // Show the next document if exist
        add(new AjaxLink<Void>("showNextDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                editor.reset(aTarget);
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements=   getListOfDocs();

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements.indexOf(bModel.getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    aTarget.appendJavaScript("alert('This is the last document!')");
                    return;
                }
                bModel.setDocument(listOfSourceDocuements.get(currentDocumentIndex + 1));

                try {
                    repository.upgradeCasAndSave(bModel.getDocument(), Mode.AUTOMATION, bModel
                            .getUser().getUsername());
                    loadDocumentAction(aTarget);
                    setCurationSegmentBeginEnd();
                    update(aTarget);

                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (IOException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (BratAnnotationException e) {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    error(e.getMessage());
                }

                finish.setModelObject(bModel);
                aTarget.add(finish.setOutputMarkupId(true));
                aTarget.add(numberOfPages);
                aTarget.add(documentNamePanel);
                annotator.bratRenderLater(aTarget);
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

        // Show the next page of this document
        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                        int address = getAddr(selectSentenceAt(mergeJCas,
                                bModel.getSentenceBeginOffset(), bModel.getSentenceEndOffset()));
                        int nextSentenceAddress = getNextPageFirstSentenceAddress(mergeJCas,
                                address, bModel.getPreferences().getWindowSize());
                        if (address != nextSentenceAddress) {
                            updateSentenceNumber(mergeJCas, nextSentenceAddress);

                            SuggestionBuilder builder = new SuggestionBuilder(repository,
                                    annotationService, userRepository);
                            curationContainer = builder.buildCurationContainer(bModel);
                            setCurationSegmentBeginEnd();
                            curationContainer.setBratAnnotatorModel(bModel);
                            update(aTarget);
                            annotator.bratRenderLater(aTarget);
                        }

                        else {
                            aTarget.appendJavaScript("alert('This is last page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // SHow the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {

                    JCas mergeJCas = null;
                    try {
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                        int previousSentenceAddress = BratAjaxCasUtil
                                .getPreviousDisplayWindowSentenceBeginAddress(mergeJCas, bModel
                                        .getSentenceAddress(), bModel.getPreferences()
                                        .getWindowSize());
                        if (bModel.getSentenceAddress() != previousSentenceAddress) {
                            updateSentenceNumber(mergeJCas, previousSentenceAddress);

                            SuggestionBuilder builder = new SuggestionBuilder(repository,
                                    annotationService, userRepository);

                            curationContainer = builder.buildCurationContainer(bModel);
                            setCurationSegmentBeginEnd();
                            curationContainer.setBratAnnotatorModel(bModel);
                            update(aTarget);
                            annotator.bratRenderLater(aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is First Page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        ;
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        mergeJCas = repository.readCorrectionCas(bModel.getDocument());

                        int address = getAddr(selectSentenceAt(mergeJCas,
                                bModel.getSentenceBeginOffset(), bModel.getSentenceEndOffset()));
                        int firstAddress = getFirstSentenceAddress(mergeJCas);

                        if (firstAddress != address) {
                            updateSentenceNumber(mergeJCas, firstAddress);

                            SuggestionBuilder builder = new SuggestionBuilder(repository,
                                    annotationService, userRepository);
                            curationContainer = builder.buildCurationContainer(bModel);
                            setCurationSegmentBeginEnd();
                            curationContainer.setBratAnnotatorModel(bModel);
                            update(aTarget);
                            annotator.bratRenderLater(aTarget);
                        }
                        else {
                            aTarget.appendJavaScript("alert('This is first page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (bModel.getDocument() != null) {
                    JCas mergeJCas = null;
                    try {
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                        mergeJCas = repository.readCorrectionCas(bModel.getDocument());
                        int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                                .getLastDisplayWindowFirstSentenceAddress(mergeJCas, bModel
                                        .getPreferences().getWindowSize());
                        if (lastDisplayWindowBeginingSentenceAddress != bModel.getSentenceAddress()) {
                            updateSentenceNumber(mergeJCas,
                                    lastDisplayWindowBeginingSentenceAddress);

                            SuggestionBuilder builder = new SuggestionBuilder(repository,
                                    annotationService, userRepository);
                            curationContainer = builder.buildCurationContainer(bModel);
                            setCurationSegmentBeginEnd();
                            curationContainer.setBratAnnotatorModel(bModel);
                            update(aTarget);
                            annotator.bratRenderLater(aTarget);

                        }
                        else {
                            aTarget.appendJavaScript("alert('This is last Page!')");
                        }
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
                else {
                    aTarget.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new AjaxLink<Void>("toggleScriptDirection")
        {
            private static final long serialVersionUID = -4332566542278611728L;

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                if (ScriptDirection.LTR.equals(bModel.getScriptDirection())) {
                    bModel.setScriptDirection(ScriptDirection.RTL);
                }
                else {
                    bModel.setScriptDirection(ScriptDirection.LTR);
                }

                try {
                    curationContainer.setBratAnnotatorModel(bModel);
                    CuratorUtil.updatePanel(aTarget, automateView, curationContainer, annotator,
                            repository, annotationSelectionByUsernameAndAddress, curationSegment,
                            annotationService, userRepository);
                }
                catch (UIMAException | ClassNotFoundException | IOException
                        | BratAnnotationException e) {
                    error(e);
                    LOG.error(e);
                }
                
                annotator.bratRenderLater(aTarget);
            }
        });
        
        add(new GuidelineModalPanel("guidelineModalPanel", new Model<ActionContext>(bModel)));
    }

    
	private List<SourceDocument> getListOfDocs() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.get(username);
		// List of all Source Documents in the project
		List<SourceDocument> listOfSourceDocuements = repository.listSourceDocuments(bModel.getProject());
		List<SourceDocument> sourceDocumentsInIgnoreState = new ArrayList<SourceDocument>();
		for (SourceDocument sourceDocument : listOfSourceDocuements) {
			if (repository.existsAnnotationDocument(sourceDocument, user) && repository
					.getAnnotationDocument(sourceDocument, user).getState().equals(AnnotationDocumentState.IGNORE)) {
				sourceDocumentsInIgnoreState.add(sourceDocument);
			}
		}

		listOfSourceDocuements.removeAll(sourceDocumentsInIgnoreState);
		return listOfSourceDocuements;
	}
    /**
     * for the first time the page is accessed, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.render(OnLoadHeaderItem.forScript(jQueryString));
        if (bModel.getProject() != null) {

            annotator.setModelObject(bModel);
            annotator.setCollection("#" + bModel.getProject().getName() + "/");
            annotator.bratInitRenderLater(response);

        }

    }

    private void loadDocumentAction(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.get(SecurityContextHolder.getContext()
                .getAuthentication().getName());

        bModel.setUser(loggedInUser);

        if (bModel.getDocument().getState().equals(SourceDocumentState.NEW)) {
            bModel.getDocument().setState(SourceDocumentStateTransition.transition(
                    SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
            repository.createSourceDocument(bModel.getDocument(), loggedInUser);
        }
        
        JCas jCas = null;
        try {
            AnnotationDocument logedInUserAnnotationDocument = repository.getAnnotationDocument(
                    bModel.getDocument(), loggedInUser);
            jCas = repository.readAnnotationCas(logedInUserAnnotationDocument);
         // upgrade this cas
            repository.upgradeCas(jCas.getCas(), logedInUserAnnotationDocument);
            repository.writeAnnotationCas(jCas, bModel.getDocument(), loggedInUser);
            
            // upgrade this automation cas
            repository.upgradeCorrectionCas(repository.readCorrectionCas(bModel.getDocument()).getCas(),  bModel.getDocument());
        }
        catch (IOException e) {
            throw e;
        }
        // Get information to be populated to bratAnnotatorModel from the JCAS of the logged in user
        //
        catch (DataRetrievalFailureException e) {

            jCas = repository.readAnnotationCas(repository.createOrGetAnnotationDocument(bModel.getDocument(), loggedInUser));
            // upgrade this cas
            repository.upgradeCas(jCas.getCas(), repository.createOrGetAnnotationDocument(bModel.getDocument(), loggedInUser));
            repository.writeAnnotationCas(jCas, bModel.getDocument(), loggedInUser);
            // This is the auto annotation, save it under CORRECTION_USER, Only if it is not created
            // by another annotator
            if (!repository.existsCorrectionCas(bModel.getDocument())) {
                repository.writeCorrectionCas(jCas, bModel.getDocument(), loggedInUser);
            }
            else {
                // upgrade this automation cas
                repository.upgradeCorrectionCas(repository.readCorrectionCas(bModel.getDocument()).getCas(),  bModel.getDocument());
            }
        }
        catch (NoResultException e) {
            jCas = repository.readAnnotationCas(repository.createOrGetAnnotationDocument(bModel.getDocument(), loggedInUser));
            // upgrade this cas
            repository.upgradeCas(jCas.getCas(), repository.createOrGetAnnotationDocument(bModel.getDocument(), loggedInUser));
            repository.writeAnnotationCas(jCas, bModel.getDocument(), loggedInUser);
            // This is the auto annotation, save it under CORRECTION_USER, Only if it is not created
            // by another annotator
            if (!repository.existsCorrectionCas(bModel.getDocument())) {
                repository.writeCorrectionCas(jCas, bModel.getDocument(), loggedInUser);
            }
            else{
                // upgrade this automation cas
                repository.upgradeCorrectionCas(repository.readCorrectionCas(bModel.getDocument()).getCas(),  bModel.getDocument());
            }
        }

        // (Re)initialize brat model after potential creating / upgrading CAS
        bModel.initForDocument(jCas, repository);

        // Load user preferences
        PreferencesUtil.setAnnotationPreference(username, repository, annotationService, bModel,
                Mode.AUTOMATION);
        // Re-render whole page as sidebar size preference may have changed
        aTarget.add(AutomationPage.this);

        // if project is changed, reset some project specific settings
        if (currentprojectId != bModel.getProject().getId()) {
            bModel.clearRememberedFeatures();
        }
        // Load constraints
        bModel.setConstraints(loadConstraints(bModel.getProject()));
        currentprojectId = bModel.getProject().getId();
        currentDocumentId = bModel.getDocument().getId();

        LOG.debug("Configured BratAnnotatorModel for user [" + bModel.getUser() + "] f:["
                + bModel.getFirstVisibleSentenceNumber() + "] l:["
                + bModel.getLastVisibleSentenceNumber() + "] s:[" + bModel.getFocusSentenceNumber()
                + "]");
    }

    private void setCurationSegmentBeginEnd()
        throws UIMAException, ClassNotFoundException, IOException
    {
        JCas jCas = repository.readAnnotationCas(bModel.getDocument(), bModel.getUser());

        final int sentenceAddress = getAddr(selectSentenceAt(jCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset()));

        final Sentence sentence = selectByAddr(jCas, Sentence.class, sentenceAddress);
        List<Sentence> followingSentences = selectFollowing(jCas, Sentence.class, sentence, bModel
                .getPreferences().getWindowSize());
        // Check also, when getting the last sentence address in the display window, if this is the
        // last sentence or the ONLY sentence in the document
        Sentence lastSentenceAddressInDisplayWindow = followingSentences.size() == 0 ? sentence
                : followingSentences.get(followingSentences.size() - 1);
        curationSegment.setBegin(sentence.getBegin());
        curationSegment.setEnd(lastSentenceAddressInDisplayWindow.getEnd());

    }

    private void updateSentenceNumber(JCas aJCas, int aAddress)
    {
        bModel.setSentenceAddress(aAddress);
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        bModel.setSentenceBeginOffset(sentence.getBegin());
        bModel.setSentenceEndOffset(sentence.getEnd());
        bModel.setFocusSentenceNumber(BratAjaxCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));

        Sentence firstSentence = selectSentenceAt(aJCas, bModel.getSentenceBeginOffset(),
                bModel.getSentenceEndOffset());
        Sentence lastSentenceInPage = getLastSentenceInDisplayWindow(aJCas,
                getAddr(firstSentence), bModel.getPreferences().getWindowSize());
        bModel.setFirstVisibleSentenceNumber(
                BratAjaxCasUtil.getSentenceNumber(aJCas, firstSentence.getBegin()));
        bModel.setLastVisibleSentenceNumber(
                BratAjaxCasUtil.getSentenceNumber(aJCas, lastSentenceInPage.getBegin()));
    }

    private void update(AjaxRequestTarget target)
    {
        JCas jCas = null;
        try {
            CuratorUtil.updatePanel(target, automateView, curationContainer, annotator, repository,
                    annotationSelectionByUsernameAndAddress, curationSegment, annotationService,
                    userRepository);

            jCas = repository.readCorrectionCas(bModel.getDocument());
        }
        catch (UIMAException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error(e.getMessage());
        }
        catch (IOException e) {
            error(e.getMessage());
        }
        catch (BratAnnotationException e) {
            error(e.getMessage());
        }

        gotoPageTextField
                .setModelObject(getFirstSentenceNumber(jCas, bModel.getSentenceAddress()) + 1);
        gotoPageAddress = getSentenceAddress(jCas, gotoPageTextField.getModelObject());

        target.add(gotoPageTextField);
        target.add(automateView);
        target.add(numberOfPages);
    }

    private ParsedConstraints loadConstraints(Project aProject)
        throws IOException
    {
        ParsedConstraints merged = null;

        for (ConstraintSet set : repository.listConstraintSets(aProject)) {
            try {
                String script = repository.readConstrainSet(set);
                ConstraintsGrammar parser = new ConstraintsGrammar(new StringReader(script));
                Parse p = parser.Parse();
                ParsedConstraints constraints = p.accept(new ParserVisitor());

                if (merged == null) {
                    merged = constraints;
                }
                else {
                    // Merge imports
                    for (Entry<String, String> e : constraints.getImports().entrySet()) {
                        // Check if the value already points to some other feature in previous
                        // constraint file(s).
                        if (merged.getImports().containsKey(e.getKey()) && !e.getValue()
                                .equalsIgnoreCase(merged.getImports().get(e.getKey()))) {
                            // If detected, notify user with proper message and abort merging
                            StringBuffer errorMessage = new StringBuffer();
                            errorMessage.append("Conflict detected in imports for key \"");
                            errorMessage.append(e.getKey());
                            errorMessage.append("\", conflicting values are \"");
                            errorMessage.append(e.getValue());
                            errorMessage.append("\" & \"");
                            errorMessage.append(merged.getImports().get(e.getKey()));
                            errorMessage.append(
                                    "\". Please contact Project Admin for correcting this. Constraints feature may not work.");
                            errorMessage.append("\nAborting Constraint rules merge!");
                            LOG.error(errorMessage.toString());
                            error(errorMessage.toString());
                            break;
                        }
                    }
                    merged.getImports().putAll(constraints.getImports());

                    // Merge scopes
                    for (Scope scope : constraints.getScopes()) {
                        Scope target = merged.getScopeByName(scope.getScopeName());
                        if (target == null) {
                            // Scope does not exist yet
                            merged.getScopes().add(scope);
                        }
                        else {
                            // Scope already exists
                            target.getRules().addAll(scope.getRules());
                        }
                    }
                }
            }
            catch (ParseException e) {
                LOG.error("Error", e);
//                aTarget.addChildren(getPage(), FeedbackPanel.class);
                error(e.getMessage());
            }
        }

        return merged;
    }

}
